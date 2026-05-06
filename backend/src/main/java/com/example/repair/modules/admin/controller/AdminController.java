package com.example.repair.modules.admin.controller;

import com.example.repair.common.api.ApiResponse;
import com.example.repair.common.exception.BizException;
import com.example.repair.common.security.UserContext;
import com.example.repair.modules.admin.dto.AdminOverviewResponse;
import com.example.repair.modules.admin.service.AdminOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
  private final AdminOverviewService overviewService;

  @GetMapping("/overview")
  public ApiResponse<AdminOverviewResponse> overview() {
    requireAdmin();
    return ApiResponse.ok(overviewService.load());
  }

  private void requireAdmin() {
    String role = UserContext.getRequired().getRole();
    if (!"ADMIN".equalsIgnoreCase(role)) {
      throw new BizException("Forbidden");
    }
  }
}
