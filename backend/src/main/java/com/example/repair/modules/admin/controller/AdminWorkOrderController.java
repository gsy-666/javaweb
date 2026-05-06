package com.example.repair.modules.admin.controller;

import com.example.repair.common.api.ApiResponse;
import com.example.repair.common.exception.BizException;
import com.example.repair.common.security.UserContext;
import com.example.repair.modules.admin.dto.AdminAssignWorkOrderRequest;
import com.example.repair.modules.admin.dto.AdminWorkerOption;
import com.example.repair.modules.admin.service.AdminWorkOrderService;
import com.example.repair.modules.workorder.entity.WorkOrder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminWorkOrderController {
  private final AdminWorkOrderService adminWorkOrderService;

  @GetMapping("/work-orders")
  public ApiResponse<List<WorkOrder>> listWorkOrders() {
    requireAdmin();
    return ApiResponse.ok(adminWorkOrderService.listAll());
  }

  @GetMapping("/workers")
  public ApiResponse<List<AdminWorkerOption>> listWorkers() {
    requireAdmin();
    return ApiResponse.ok(adminWorkOrderService.listWorkers());
  }

  @PostMapping("/work-orders/{id}/assign")
  public ApiResponse<Void> assign(@PathVariable Long id, @RequestBody AdminAssignWorkOrderRequest req) {
    requireAdmin();
    adminWorkOrderService.assign(id, req == null ? null : req.getWorkerId());
    return ApiResponse.ok();
  }

  private void requireAdmin() {
    String role = UserContext.getRequired().getRole();
    if (!"ADMIN".equalsIgnoreCase(role)) {
      throw new BizException("Forbidden");
    }
  }
}
