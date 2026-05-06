package com.example.repair.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthLoginResponse {
  private Long userId;
  private String role;
  private String displayName;
  private String token;
}
