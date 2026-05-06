package com.example.repair.modules.worker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("worker_profile")
public class WorkerProfile {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long userId;
  private String tradeCode;
  private Integer acceptOrders;
  private Double lastLng;
  private Double lastLat;
  private LocalDateTime lastLocationAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

