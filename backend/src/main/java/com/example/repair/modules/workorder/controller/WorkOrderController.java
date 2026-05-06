package com.example.repair.modules.workorder.controller;

import com.example.repair.common.api.ApiResponse;
import com.example.repair.modules.workorder.dto.WorkOrderCreateRequest;
import com.example.repair.modules.workorder.dto.WorkOrderDetailResponse;
import com.example.repair.modules.workorder.dto.WorkOrderEtaRequest;
import com.example.repair.modules.workorder.dto.WorkOrderProgressRequest;
import com.example.repair.modules.workorder.dto.WorkOrderRateRequest;
import com.example.repair.modules.workorder.entity.WorkOrder;
import com.example.repair.modules.workorder.service.WorkOrderService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {
  private final WorkOrderService workOrderService;

  @PostMapping
  public ApiResponse<WorkOrder> create(@Valid @RequestBody WorkOrderCreateRequest req) {
    return ApiResponse.ok(workOrderService.create(req));
  }

  @GetMapping
  public ApiResponse<List<WorkOrder>> listMine() {
    return ApiResponse.ok(workOrderService.listMine());
  }

  @GetMapping("/{id}")
  public ApiResponse<WorkOrderDetailResponse> detail(@PathVariable Long id) {
    return ApiResponse.ok(workOrderService.detail(id));
  }

  @PostMapping("/{id}/cancel")
  public ApiResponse<Void> cancel(@PathVariable Long id) {
    workOrderService.cancel(id);
    return ApiResponse.ok();
  }

  @PostMapping("/{id}/delete")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    workOrderService.deleteMine(id);
    return ApiResponse.ok();
  }

  @PostMapping("/{id}/accept")
  public ApiResponse<Void> accept(@PathVariable Long id) {
    workOrderService.accept(id);
    return ApiResponse.ok();
  }

  @PostMapping("/{id}/reject")
  public ApiResponse<Void> reject(@PathVariable Long id, @RequestParam(required = false) String reason) {
    workOrderService.reject(id, reason);
    return ApiResponse.ok();
  }

  @PostMapping("/{id}/progress")
  public ApiResponse<Void> progress(@PathVariable Long id, @Valid @RequestBody WorkOrderProgressRequest req) {
    workOrderService.addProgress(id, req);
    return ApiResponse.ok();
  }

  @PostMapping("/{id}/finish")
  public ApiResponse<Void> finish(@PathVariable Long id, @RequestParam(required = false) String message) {
    workOrderService.finish(id, message);
    return ApiResponse.ok();
  }

  @PostMapping("/{id}/rating")
  public ApiResponse<Void> rating(@PathVariable Long id, @Valid @RequestBody WorkOrderRateRequest req) {
    workOrderService.rate(id, req);
    return ApiResponse.ok();
  }

  @PostMapping("/{id}/confirm-fixed")
  public ApiResponse<Void> confirmFixed(@PathVariable Long id) {
    workOrderService.confirmFixed(id);
    return ApiResponse.ok();
  }

  @PostMapping("/{id}/eta")
  public ApiResponse<Void> setEta(@PathVariable Long id, @Valid @RequestBody WorkOrderEtaRequest req) {
    try {
      LocalDateTime etaAt = req == null || req.getEtaAt() == null ? null : LocalDateTime.parse(req.getEtaAt());
      workOrderService.setEta(id, etaAt);
      return ApiResponse.ok();
    } catch (DateTimeParseException e) {
      throw new com.example.repair.common.exception.BizException("Invalid etaAt");
    }
  }
}

