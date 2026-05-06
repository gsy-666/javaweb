package com.example.repair.modules.worker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkerLocationRequest {
  @NotBlank private String tradeCode;
  @NotNull private Double lng;
  @NotNull private Double lat;
  private Boolean acceptOrders = true;
}

