package com.example.repair.modules.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.repair.common.exception.BizException;
import com.example.repair.modules.admin.dto.AdminWorkerOption;
import com.example.repair.modules.auth.entity.SysRole;
import com.example.repair.modules.auth.entity.SysUser;
import com.example.repair.modules.auth.entity.SysUserRole;
import com.example.repair.modules.auth.mapper.SysRoleMapper;
import com.example.repair.modules.auth.mapper.SysUserMapper;
import com.example.repair.modules.auth.mapper.SysUserRoleMapper;
import com.example.repair.modules.timeout.model.TimeoutStage;
import com.example.repair.modules.timeout.service.TimeoutQueueService;
import com.example.repair.modules.workorder.entity.WorkOrder;
import com.example.repair.modules.workorder.entity.WorkOrderProgress;
import com.example.repair.modules.workorder.mapper.WorkOrderMapper;
import com.example.repair.modules.workorder.mapper.WorkOrderProgressMapper;
import com.example.repair.modules.worker.entity.WorkerProfile;
import com.example.repair.modules.worker.mapper.WorkerProfileMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminWorkOrderService {
  private final WorkOrderMapper workOrderMapper;
  private final WorkOrderProgressMapper progressMapper;
  private final WorkerProfileMapper workerProfileMapper;
  private final SysUserMapper sysUserMapper;
  private final SysUserRoleMapper sysUserRoleMapper;
  private final SysRoleMapper sysRoleMapper;
  private final TimeoutQueueService timeoutQueueService;

  public List<WorkOrder> listAll() {
    return workOrderMapper.selectList(
        new LambdaQueryWrapper<WorkOrder>()
            .isNull(WorkOrder::getDeletedAt)
            .orderByDesc(WorkOrder::getCreatedAt));
  }

  public List<AdminWorkerOption> listWorkers() {
    SysRole workerRole =
        sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, "WORKER"));
    if (workerRole == null) return List.of();

    List<SysUserRole> roles =
        sysUserRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, workerRole.getId()));
    List<Long> workerIds = roles.stream().map(SysUserRole::getUserId).distinct().toList();
    if (workerIds.isEmpty()) return List.of();

    List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, workerIds));
    List<WorkerProfile> profiles =
        workerProfileMapper.selectList(
            new LambdaQueryWrapper<WorkerProfile>().in(WorkerProfile::getUserId, workerIds));

    Map<Long, SysUser> userMap = users.stream().collect(Collectors.toMap(SysUser::getId, x -> x, (a, b) -> a));
    Map<Long, WorkerProfile> profileMap = profiles.stream().collect(Collectors.toMap(WorkerProfile::getUserId, x -> x, (a, b) -> a));

    return workerIds.stream()
        .map(
            uid -> {
              SysUser u = userMap.get(uid);
              WorkerProfile p = profileMap.get(uid);
              String name =
                  u == null
                      ? ("#" + uid)
                      : (u.getDisplayName() == null || u.getDisplayName().isBlank()
                          ? u.getUsername()
                          : u.getDisplayName());
              return new AdminWorkerOption(uid, name, p == null ? null : p.getTradeCode(), p == null ? 0 : p.getAcceptOrders());
            })
        .toList();
  }

  @Transactional
  public void assign(Long workOrderId, Long workerId) {
    if (workerId == null) throw new BizException("workerId required");

    WorkOrder wo = workOrderMapper.selectById(workOrderId);
    if (wo == null) throw new BizException("Work order not found");
    if ("CLOSED".equals(wo.getStatus()) || "CANCELLED".equals(wo.getStatus())) {
      throw new BizException("Work order is closed");
    }

    SysRole workerRole =
        sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, "WORKER"));
    if (workerRole == null) throw new BizException("Worker role not found");

    Long cnt =
        sysUserRoleMapper.selectCount(
            new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, workerId)
                .eq(SysUserRole::getRoleId, workerRole.getId()));
    boolean isWorker = cnt != null && cnt > 0;
    if (!isWorker) throw new BizException("Target user is not a worker");

    String from = wo.getStatus();
    LocalDateTime now = LocalDateTime.now();

    wo.setAssignedWorkerId(workerId);
    wo.setAssignedAt(now);
    wo.setStatus("ASSIGNED");
    workOrderMapper.updateById(wo);

    WorkOrderProgress p = new WorkOrderProgress();
    p.setWorkOrderId(workOrderId);
    p.setFromStatus(from);
    p.setToStatus("ASSIGNED");
    p.setMessage("管理员分配：已指派给维修人员#" + workerId);
    p.setOperatorId(null);
    p.setCreatedAt(now);
    progressMapper.insert(p);

    timeoutQueueService.cancel(workOrderId, TimeoutStage.ASSIGN);
    timeoutQueueService.scheduleAccept(workOrderId, now);
  }
}
