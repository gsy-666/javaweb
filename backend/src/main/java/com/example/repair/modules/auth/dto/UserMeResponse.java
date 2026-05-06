package com.example.repair.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserMeResponse {
  private Long userId;
  private String role;
  private String displayName;
  private String account;
}
