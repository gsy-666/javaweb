package com.example.repair.modules.auth.controller;

import com.example.repair.common.api.ApiResponse;
import com.example.repair.common.security.UserContext;
import com.example.repair.modules.auth.dto.UserMeResponse;
import com.example.repair.modules.auth.entity.SysUser;
import com.example.repair.modules.auth.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final SysUserMapper sysUserMapper;

  @GetMapping("/me")
  public ApiResponse<UserMeResponse> me() {
    UserContext.UserPrincipal p = UserContext.getRequired();
    SysUser u = sysUserMapper.selectById(p.getUserId());
    String displayName = u == null ? null : u.getDisplayName();
    String account = u == null ? null : (u.getUsername() != null && !u.getUsername().isBlank() ? u.getUsername() : u.getPhone());
    return ApiResponse.ok(new UserMeResponse(p.getUserId(), p.getRole(), displayName, account));
  }
}
