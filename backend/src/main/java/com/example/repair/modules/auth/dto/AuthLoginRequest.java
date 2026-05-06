package com.example.repair.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthLoginRequest {

  @NotBlank
  private String account;

  @NotBlank
  @Size(min = 6)
  private String password;
}
