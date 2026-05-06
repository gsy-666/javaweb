package com.example.repair.modules.timeout.service;

import com.example.repair.modules.dispatch.DispatchService;
import com.example.repair.modules.timeout.entity.WorkOrderEscalation;
import com.example.repair.modules.timeout.mapper.WorkOrderEscalationMapper;
import com.example.repair.modules.timeout.model.TimeoutStage;
import com.example.repair.modules.workorder.entity.WorkOrder;
import com.example.repair.modules.workorder.entity.WorkOrderProgress;
import com.example.repair.modules.workorder.mapper.WorkOrderMapper;
import com.example.repair.modules.workorder.mapper.WorkOrderProgressMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeoutStageProcessor {
  private final StringRedisTemplate redis;
  private final TimeoutQueueService queue;
  private final WorkOrderMapper workOrderMapper;
  private final WorkOrderProgressMapper progressMapper;
  private final WorkOrderEscalationMapper escalationMapper;
  private final DispatchService dispatchService;
  private final NotificationService notificationService;

  @Transactional
  public void processMember(String member) {
    Parsed p = parse(member);
    if (p == null) return;

    String lockKey = "lock:timeout:{" + p.workOrderId + "}";
    Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(10));
    if (!Boolean.TRUE.equals(locked)) return;
    try {
      WorkOrder wo = workOrderMapper.selectById(p.workOrderId);
      if (wo == null) return;
      if ("CANCELLED".equals(wo.getStatus()) || "CLOSED".equals(wo.getStatus())) {
        queue.cancelAll(p.workOrderId);
        return;
      }

      switch (p.stage) {
        case ASSIGN -> handleAssignTimeout(wo);
        case ACCEPT -> handleAcceptTimeout(wo);
        case FIRST_UPDATE -> handleFirstUpdateTimeout(wo);
        case FINISH -> handleFinishTimeout(wo);
      }
    } finally {
      redis.delete(lockKey);
    }
  }

  private void handleAssignTimeout(WorkOrder wo) {
    if (!"NEW".equals(wo.getStatus())) return;
    int lvl = bumpEscalationLevel(wo, TimeoutStage.ASSIGN);
    if (lvl == 1) {
      insertProgress(wo.getId(), "NEW", "NEW", "超时升级(L1)：等待派单过久，系统将重试派单");
      dispatchService.triggerDispatch(wo.getId());
      queue.scheduleAssign(wo.getId(), LocalDateTime.now());
      return;
    }
    if (lvl == 2) {
      insertProgress(wo.getId(), "NEW", "NEW", "超时升级(L2)：派单失败，系统将扩大范围重试");
      dispatchService.triggerDispatch(wo.getId());
      queue.scheduleAssign(wo.getId(), LocalDateTime.now());
      return;
    }
    notificationService.notifyAdmin("工单#" + wo.getId() + " 长时间无法派单，请人工介入");
  }

  private void handleAcceptTimeout(WorkOrder wo) {
    if (!"ASSIGNED".equals(wo.getStatus())) return;
    int lvl = bumpEscalationLevel(wo, TimeoutStage.ACCEPT);
    Long workerId = wo.getAssignedWorkerId();
    if (lvl == 1) {
      if (workerId != null) notificationService.notifyWorker(workerId, "你有待接单工单#" + wo.getId() + "已超时，请尽快处理");
      insertProgress(wo.getId(), "ASSIGNED", "ASSIGNED", "超时升级(L1)：接单超时，已提醒维修人员");
      queue.scheduleAccept(wo.getId(), LocalDateTime.now());
      return;
    }
    if (lvl == 2) {
      insertProgress(wo.getId(), "ASSIGNED", "NEW", "超时升级(L2)：接单超时，系统自动改派");
      if (workerId != null) dispatchService.cooldownWorker(workerId);
      wo.setAssignedWorkerId(null);
      wo.setAssignedAt(null);
      wo.setStatus("NEW");
      workOrderMapper.updateById(wo);
      dispatchService.triggerDispatch(wo.getId());
      queue.scheduleAssign(wo.getId(), LocalDateTime.now());
      return;
    }
    notificationService.notifyAdmin("工单#" + wo.getId() + " 接单多次超时，请人工处理");
  }

  private void handleFirstUpdateTimeout(WorkOrder wo) {
    if (!"ACCEPTED".equals(wo.getStatus())) return;
    int lvl = bumpEscalationLevel(wo, TimeoutStage.FIRST_UPDATE);
    Long workerId = wo.getAssignedWorkerId();
    if (lvl <= 2) {
      if (workerId != null) notificationService.notifyWorker(workerId, "工单#" + wo.getId() + " 接单后未更新进度已超时，请更新");
      insertProgress(wo.getId(), "ACCEPTED", "ACCEPTED", "超时升级(L" + lvl + ")：首次进度超时，已提醒维修人员");
      queue.scheduleFirstUpdate(wo.getId(), LocalDateTime.now());
      return;
    }
    notificationService.notifyAdmin("工单#" + wo.getId() + " 首次进度长期未更新，请人工介入");
  }

  private void handleFinishTimeout(WorkOrder wo) {
    if (!"ACCEPTED".equals(wo.getStatus()) && !"IN_PROGRESS".equals(wo.getStatus()) && !"WAIT_USER".equals(wo.getStatus())) {
      return;
    }
    int lvl = bumpEscalationLevel(wo, TimeoutStage.FINISH);
    notificationService.notifyAdmin("工单#" + wo.getId() + " 完工超时(L" + lvl + ")，请关注");
    insertProgress(wo.getId(), wo.getStatus(), wo.getStatus(), "超时升级(L" + lvl + ")：完工超时，已通知管理员");
    queue.scheduleFinish(wo.getId(), LocalDateTime.now());
  }

  private int bumpEscalationLevel(WorkOrder wo, TimeoutStage stage) {
    Integer cur = wo.getEscalationLevel();
    int lvl = (cur == null ? 0 : cur) + 1;
    wo.setEscalationLevel(lvl);
    workOrderMapper.updateById(wo);

    WorkOrderEscalation e = new WorkOrderEscalation();
    e.setWorkOrderId(wo.getId());
    e.setLevel(lvl);
    e.setStage(stage.name());
    e.setAction(lvl == 1 ? "REMIND" : (lvl == 2 ? "REDISPATCH" : "NOTIFY"));
    e.setNote(null);
    e.setCreatedAt(LocalDateTime.now());
    escalationMapper.insert(e);
    return lvl;
  }

  private void insertProgress(Long workOrderId, String from, String to, String msg) {
    WorkOrderProgress p = new WorkOrderProgress();
    p.setWorkOrderId(workOrderId);
    p.setFromStatus(from);
    p.setToStatus(to);
    p.setMessage(msg);
    p.setOperatorId(null);
    p.setCreatedAt(LocalDateTime.now());
    progressMapper.insert(p);
  }

  private Parsed parse(String member) {
    if (member == null) return null;
    String[] arr = member.split(":");
    if (arr.length != 2) return null;
    try {
      long woId = Long.parseLong(arr[0]);
      TimeoutStage stage = TimeoutStage.valueOf(arr[1]);
      return new Parsed(woId, stage);
    } catch (Exception e) {
      log.warn("Invalid delay member: {}", member);
      return null;
    }
  }

  private record Parsed(long workOrderId, TimeoutStage stage) {}
}

