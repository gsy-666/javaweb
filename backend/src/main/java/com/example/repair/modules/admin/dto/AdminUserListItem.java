package com.example.repair.modules.admin.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserListItem {
  private Long userId;
  private String role;
  private String account;
  private String displayName;
  private String phone;
  private Integer enabled;
  private String tradeCode;
  private Integer acceptOrders;
  private LocalDateTime createdAt;
}
