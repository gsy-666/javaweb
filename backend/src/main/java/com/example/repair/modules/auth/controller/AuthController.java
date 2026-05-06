package com.example.repair.modules.auth.controller;

import com.example.repair.common.api.ApiResponse;
import com.example.repair.modules.auth.dto.AuthLoginRequest;
import com.example.repair.modules.auth.dto.AuthLoginResponse;
import com.example.repair.modules.auth.dto.AuthRegisterRequest;
import com.example.repair.modules.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  public ApiResponse<AuthLoginResponse> register(@Valid @RequestBody AuthRegisterRequest req) {
    return ApiResponse.ok(authService.register(req));
  }

  @PostMapping("/login")
  public ApiResponse<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest req) {
    return ApiResponse.ok(authService.login(req));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(HttpServletRequest request) {
    String auth = request.getHeader("Authorization");
    String token = null;
    if (auth != null && auth.startsWith("Bearer ")) {
      token = auth.substring("Bearer ".length()).trim();
    }
    authService.logout(token);
    return ApiResponse.ok();
  }
}
