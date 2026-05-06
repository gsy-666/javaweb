package com.example.repair.modules.workorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.repair.common.exception.BizException;
import com.example.repair.common.security.UserContext;
import com.example.repair.modules.dispatch.DispatchService;
import com.example.repair.modules.auth.mapper.SysUserMapper;
import com.example.repair.modules.lbs.service.LbsService;
import com.example.repair.modules.timeout.model.TimeoutStage;
import com.example.repair.modules.timeout.service.TimeoutQueueService;
import com.example.repair.modules.workorder.dto.WorkOrderCreateRequest;
import com.example.repair.modules.workorder.dto.WorkOrderDetailResponse;
import com.example.repair.modules.workorder.dto.WorkOrderProgressRequest;
import com.example.repair.modules.workorder.dto.WorkOrderRateRequest;
import com.example.repair.modules.workorder.entity.WorkOrder;
import com.example.repair.modules.workorder.entity.WorkOrderImage;
import com.example.repair.modules.workorder.entity.WorkOrderProgress;
import com.example.repair.modules.workorder.entity.WorkOrderRating;
import com.example.repair.modules.workorder.mapper.WorkOrderImageMapper;
import com.example.repair.modules.workorder.mapper.WorkOrderMapper;
import com.example.repair.modules.workorder.mapper.WorkOrderProgressMapper;
import com.example.repair.modules.workorder.mapper.WorkOrderRatingMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkOrderService {
  private final WorkOrderMapper workOrderMapper;
  private final WorkOrderProgressMapper progressMapper;
  private final WorkOrderRatingMapper ratingMapper;
  private final WorkOrderImageMapper imageMapper;
  private final DispatchService dispatchService;
  private final SysUserMapper sysUserMapper;
  private final LbsService lbsService;
  private final TimeoutQueueService timeoutQueueService;

  @Transactional
  public WorkOrder create(WorkOrderCreateRequest req) {
    Long requesterId = UserContext.getRequired().getUserId();
    LocalDateTime now = LocalDateTime.now();
    WorkOrder wo = new WorkOrder();
    wo.setCode(genCode());
    wo.setRequesterId(requesterId);
    wo.setTradeCode(req.getTradeCode());
    wo.setDescription(req.getDescription());
    wo.setBuildingId(req.getBuildingId());
    wo.setAreaId(req.getAreaId());
    String addr = req.getAddress();
    if (addr == null || addr.isBlank()) {
      addr = lbsService.reverseGeocode(req.getLng(), req.getLat());
    }
    if (addr == null || addr.isBlank()) {
      addr = "(" + req.getLng() + "," + req.getLat() + ")";
    }
    wo.setAddress(addr);
    wo.setLng(req.getLng());
    wo.setLat(req.getLat());
    wo.setPriority(req.getPriority() == null ? 0 : req.getPriority());
    wo.setStatus("NEW");
    wo.setEscalationLevel(0);
    wo.setCreatedAt(now);
    workOrderMapper.insert(wo);

    if (req.getImageUrls() != null) {
      for (String url : req.getImageUrls()) {
        if (url == null || url.isBlank()) continue;
        WorkOrderImage img = new WorkOrderImage();
        img.setWorkOrderId(wo.getId());
        img.setKind("REQUEST");
        img.setUrl(url);
        img.setCreatedAt(now);
        imageMapper.insert(img);
      }
    }

    insertProgress(wo.getId(), null, "NEW", "工单已创建");
    timeoutQueueService.scheduleAssign(wo.getId(), now);

    dispatchService.triggerDispatch(wo.getId());
    return wo;
  }

  public List<WorkOrder> listMine() {
    UserContext.UserPrincipal p = UserContext.getRequired();
    String role = p.getRole();
    LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<>();
    qw.isNull(WorkOrder::getDeletedAt);
    if ("ADMIN".equalsIgnoreCase(role)) {
      // no filter
    } else if ("WORKER".equalsIgnoreCase(role)) {
      qw.eq(WorkOrder::getAssignedWorkerId, p.getUserId());
    } else {
      qw.eq(WorkOrder::getRequesterId, p.getUserId());
    }
    qw.orderByDesc(WorkOrder::getCreatedAt);
    return workOrderMapper.selectList(qw);
  }

  public WorkOrderDetailResponse detail(Long id) {
    WorkOrder wo = getRequired(id);
    if (wo.getDeletedAt() != null) throw new BizException("Work order not found");
    List<WorkOrderImage> images =
        imageMapper.selectList(
            new LambdaQueryWrapper<WorkOrderImage>()
                .eq(WorkOrderImage::getWorkOrderId, id)
                .orderByAsc(WorkOrderImage::getCreatedAt));
    List<WorkOrderProgress> progress =
        progressMapper.selectList(
            new LambdaQueryWrapper<WorkOrderProgress>()
                .eq(WorkOrderProgress::getWorkOrderId, id)
                .orderByAsc(WorkOrderProgress::getCreatedAt));
    WorkOrderRating rating =
        ratingMapper.selectOne(new LambdaQueryWrapper<WorkOrderRating>().eq(WorkOrderRating::getWorkOrderId, id));

    UserContext.UserPrincipal p = UserContext.getRequired();
    String role = p.getRole();

    String requesterPhone = null;
    String workerPhone = null;

    if ("ADMIN".equalsIgnoreCase(role) || "WORKER".equalsIgnoreCase(role)) {
      requesterPhone = loadUserPhone(wo.getRequesterId());
    }
    if ("ADMIN".equalsIgnoreCase(role) || "USER".equalsIgnoreCase(role)) {
      if (wo.getAssignedWorkerId() != null) {
        workerPhone = loadUserPhone(wo.getAssignedWorkerId());
      }
    }

    return new WorkOrderDetailResponse(wo, images, progress, rating, requesterPhone, workerPhone);
  }

  private String loadUserPhone(Long userId) {
    if (userId == null) return null;
    com.example.repair.modules.auth.entity.SysUser u = sysUserMapper.selectById(userId);
    if (u == null) return null;
    String phone = u.getPhone();
    if (phone == null || phone.isBlank()) return null;
    return phone;
  }

  @Transactional
  public void cancel(Long id) {
    WorkOrder wo = getRequired(id);
    Long userId = UserContext.getRequired().getUserId();
    if (!userId.equals(wo.getRequesterId())) {
      throw new BizException("Only requester can cancel");
    }
    if ("CANCELLED".equals(wo.getStatus()) || "CLOSED".equals(wo.getStatus())) {
      return;
    }
    String from = wo.getStatus();
    Long assignedWorkerId = wo.getAssignedWorkerId();
    wo.setStatus("CANCELLED");
    wo.setCancelledAt(LocalDateTime.now());
    workOrderMapper.updateById(wo);
    insertProgress(id, from, "CANCELLED", "用户取消工单");
    timeoutQueueService.cancelAll(id);

    if (assignedWorkerId != null) {
      dispatchService.decreaseActiveCount(assignedWorkerId);
    }
  }

  @Transactional
  public void accept(Long id) {
    WorkOrder wo = getRequired(id);
    Long workerId = UserContext.getRequired().getUserId();
    if (wo.getAssignedWorkerId() == null || !workerId.equals(wo.getAssignedWorkerId())) {
      throw new BizException("Not assigned to you");
    }
    if (!"ASSIGNED".equals(wo.getStatus())) {
      throw new BizException("Work order not in ASSIGNED");
    }
    wo.setStatus("ACCEPTED");
    wo.setAcceptedAt(LocalDateTime.now());
    workOrderMapper.updateById(wo);
    insertProgress(id, "ASSIGNED", "ACCEPTED", "维修人员已接单");

    timeoutQueueService.cancel(id, TimeoutStage.ACCEPT);
    timeoutQueueService.scheduleFirstUpdate(id, wo.getAcceptedAt());
    timeoutQueueService.scheduleFinish(id, wo.getAcceptedAt());
  }

  @Transactional
  public void setEta(Long id, LocalDateTime etaAt) {
    WorkOrder wo = getRequired(id);
    Long workerId = UserContext.getRequired().getUserId();
    if (wo.getAssignedWorkerId() == null || !workerId.equals(wo.getAssignedWorkerId())) {
      throw new BizException("Not assigned to you");
    }
    if (!List.of("ASSIGNED", "ACCEPTED", "IN_PROGRESS", "WAIT_USER").contains(wo.getStatus())) {
      throw new BizException("Work order not in ETA-settable status");
    }
    if (etaAt == null) throw new BizException("etaAt required");

    String from = wo.getStatus();
    wo.setEtaAt(etaAt);
    workOrderMapper.updateById(wo);
    insertProgress(id, from, from, "维修人员设置预计上门时间：" + etaAt);
  }

  @Transactional
  public void reject(Long id, String reason) {
    WorkOrder wo = getRequired(id);
    Long workerId = UserContext.getRequired().getUserId();
    if (wo.getAssignedWorkerId() == null || !workerId.equals(wo.getAssignedWorkerId())) {
      throw new BizException("Not assigned to you");
    }
    if (!"ASSIGNED".equals(wo.getStatus())) {
      throw new BizException("Work order not in ASSIGNED");
    }
    wo.setAssignedWorkerId(null);
    wo.setAssignedAt(null);
    wo.setStatus("NEW");
    workOrderMapper.updateById(wo);
    insertProgress(id, "ASSIGNED", "NEW", "维修人员拒单：" + (reason == null ? "" : reason));
    dispatchService.cooldownWorker(workerId);
    timeoutQueueService.cancel(id, TimeoutStage.ACCEPT);
    timeoutQueueService.scheduleAssign(id, LocalDateTime.now());
    dispatchService.triggerDispatch(id);
  }

  @Transactional
  public void addProgress(Long id, WorkOrderProgressRequest req) {
    WorkOrder wo = getRequired(id);
    Long operatorId = UserContext.getRequired().getUserId();
    if (wo.getAssignedWorkerId() == null || !operatorId.equals(wo.getAssignedWorkerId())) {
      throw new BizException("Only assigned worker can update progress");
    }
    if (!List.of("ACCEPTED", "IN_PROGRESS", "WAIT_USER").contains(wo.getStatus())) {
      throw new BizException("Work order not in progress-able status");
    }
    String from = wo.getStatus();
    if ("ACCEPTED".equals(from)) {
      wo.setStatus("IN_PROGRESS");
      workOrderMapper.updateById(wo);
      insertProgress(id, "ACCEPTED", "IN_PROGRESS", req.getMessage());
      timeoutQueueService.cancel(id, TimeoutStage.FIRST_UPDATE);
      return;
    }
    insertProgress(id, from, from, req.getMessage());
  }

  @Transactional
  public void finish(Long id, String message) {
    WorkOrder wo = getRequired(id);
    Long operatorId = UserContext.getRequired().getUserId();
    if (wo.getAssignedWorkerId() == null || !operatorId.equals(wo.getAssignedWorkerId())) {
      throw new BizException("Only assigned worker can finish");
    }
    if (!List.of("ACCEPTED", "IN_PROGRESS", "WAIT_USER").contains(wo.getStatus())) {
      throw new BizException("Work order not in finish-able status");
    }
    String from = wo.getStatus();
    wo.setStatus("DONE_WAIT_CONFIRM");
    wo.setFinishedAt(LocalDateTime.now());
    workOrderMapper.updateById(wo);
    insertProgress(id, from, "DONE_WAIT_CONFIRM", message == null ? "维修已完工" : message);
    timeoutQueueService.cancel(id, TimeoutStage.FINISH);
    timeoutQueueService.cancel(id, TimeoutStage.FIRST_UPDATE);
  }

  @Transactional
  public void rate(Long id, WorkOrderRateRequest req) {
    WorkOrder wo = getRequired(id);
    Long userId = UserContext.getRequired().getUserId();
    if (!userId.equals(wo.getRequesterId())) {
      throw new BizException("Only requester can rate");
    }
    if (!"DONE_WAIT_RATE".equals(wo.getStatus()) && !"CLOSED".equals(wo.getStatus())) {
      throw new BizException("Work order not ready to rate");
    }
    WorkOrderRating existed =
        ratingMapper.selectOne(
            new LambdaQueryWrapper<WorkOrderRating>().eq(WorkOrderRating::getWorkOrderId, id));
    if (existed != null) {
      throw new BizException("Already rated");
    }
    WorkOrderRating r = new WorkOrderRating();
    r.setWorkOrderId(id);
    r.setRaterId(userId);
    r.setStars(req.getStars());
    if (req.getTags() != null && !req.getTags().isEmpty()) {
      r.setTags(String.join(",", req.getTags()));
    }
    r.setComment(req.getComment());
    r.setCreatedAt(LocalDateTime.now());
    ratingMapper.insert(r);

    Long assignedWorkerId = wo.getAssignedWorkerId();
    wo.setStatus("CLOSED");
    wo.setClosedAt(LocalDateTime.now());
    workOrderMapper.updateById(wo);
    insertProgress(id, "DONE_WAIT_RATE", "CLOSED", "用户已评价：" + req.getStars() + " 星");
    timeoutQueueService.cancelAll(id);

    if (assignedWorkerId != null) {
      dispatchService.decreaseActiveCount(assignedWorkerId);
    }
  }

  @Transactional
  public void confirmFixed(Long id) {
    WorkOrder wo = getRequired(id);
    Long userId = UserContext.getRequired().getUserId();
    if (!userId.equals(wo.getRequesterId())) {
      throw new BizException("Only requester can confirm");
    }
    if (!"DONE_WAIT_CONFIRM".equals(wo.getStatus())) {
      throw new BizException("Work order not in DONE_WAIT_CONFIRM");
    }

    wo.setStatus("DONE_WAIT_RATE");
    workOrderMapper.updateById(wo);
    insertProgress(id, "DONE_WAIT_CONFIRM", "DONE_WAIT_RATE", "用户确认已修好");
  }

  @Transactional
  public void deleteMine(Long id) {
    WorkOrder wo = getRequired(id);
    Long userId = UserContext.getRequired().getUserId();
    if (!userId.equals(wo.getRequesterId())) {
      throw new BizException("Only requester can delete");
    }
    if (wo.getDeletedAt() != null) return;

    if (!List.of("NEW", "ASSIGNED", "CANCELLED").contains(wo.getStatus())) {
      throw new BizException("Only NEW/ASSIGNED/CANCELLED can be deleted");
    }

    wo.setDeletedAt(LocalDateTime.now());
    workOrderMapper.updateById(wo);
    timeoutQueueService.cancelAll(id);
  }

  private WorkOrder getRequired(Long id) {
    WorkOrder wo = workOrderMapper.selectById(id);
    if (wo == null) throw new BizException("Work order not found");
    return wo;
  }

  private void insertProgress(Long workOrderId, String from, String to, String msg) {
    WorkOrderProgress p = new WorkOrderProgress();
    p.setWorkOrderId(workOrderId);
    p.setFromStatus(from);
    p.setToStatus(to);
    p.setMessage(msg);
    p.setCreatedAt(LocalDateTime.now());
    try {
      p.setOperatorId(UserContext.getRequired().getUserId());
    } catch (Exception ignored) {
      p.setOperatorId(null);
    }
    progressMapper.insert(p);
  }

  private String genCode() {
    return "WO" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
  }
}

