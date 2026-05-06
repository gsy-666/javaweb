package com.example.repair.modules.admin.controller;

import com.example.repair.common.api.ApiResponse;
import com.example.repair.common.exception.BizException;
import com.example.repair.common.security.UserContext;
import com.example.repair.modules.admin.dto.AdminUserListItem;
import com.example.repair.modules.admin.service.AdminUserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {
  private final AdminUserService adminUserService;

  @GetMapping("/users")
  public ApiResponse<List<AdminUserListItem>> listUsers(@RequestParam(required = false) String role) {
    requireAdmin();
    return ApiResponse.ok(adminUserService.listUsers(role));
  }

  private void requireAdmin() {
    String role = UserContext.getRequired().getRole();
    if (!"ADMIN".equalsIgnoreCase(role)) {
      throw new BizException("Forbidden");
    }
  }
}
