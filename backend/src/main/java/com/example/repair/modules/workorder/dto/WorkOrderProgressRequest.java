package com.example.repair.modules.workorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorkOrderProgressRequest {
  @NotBlank
  @Size(max = 1000)
  private String message;
}

