package com.example.repair.modules.chat.service;

import com.example.repair.common.exception.BizException;
import com.example.repair.common.security.UserContext;
import com.example.repair.modules.workorder.entity.WorkOrder;
import com.example.repair.modules.workorder.mapper.WorkOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatAuthService {
  private final WorkOrderMapper workOrderMapper;

  public void requireCanAccessWorkOrder(Long workOrderId) {
    UserContext.UserPrincipal p = UserContext.getOptional();
    if (p == null) throw new BizException("Forbidden");
    requireCanAccessWorkOrder(workOrderId, p.getUserId(), p.getRole());
  }

  public void requireCanAccessWorkOrder(Long workOrderId, Long userId) {
    UserContext.UserPrincipal p = UserContext.getOptional();
    String role = p == null ? null : p.getRole();
    requireCanAccessWorkOrder(workOrderId, userId, role);
  }

  private void requireCanAccessWorkOrder(Long workOrderId, Long userId, String role) {
    if (userId == null) throw new BizException("Forbidden");

    if (role != null && !role.isBlank() && "ADMIN".equalsIgnoreCase(role)) return;

    WorkOrder wo = workOrderMapper.selectById(workOrderId);
    if (wo == null) throw new BizException("工单不存在");

    if (wo.getRequesterId() != null && wo.getRequesterId().equals(userId)) return;
    if (wo.getAssignedWorkerId() != null && wo.getAssignedWorkerId().equals(userId)) return;

    throw new BizException("Forbidden");
  }
}
