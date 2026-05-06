package com.example.repair.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRegisterRequest {

  @NotBlank
  private String account;

  @NotBlank
  @Size(min = 6)
  private String password;

  private String displayName;

  @NotNull
  private String role;
}
