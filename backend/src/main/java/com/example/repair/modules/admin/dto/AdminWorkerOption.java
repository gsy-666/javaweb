package com.example.repair.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminWorkerOption {
  private Long userId;
  private String displayName;
  private String tradeCode;
  private Integer acceptOrders;
}
