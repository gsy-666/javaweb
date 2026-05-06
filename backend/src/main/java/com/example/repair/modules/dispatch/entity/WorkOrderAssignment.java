package com.example.repair.modules.dispatch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("work_order_assignment")
public class WorkOrderAssignment {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long workOrderId;
  private Long workerId;
  private Double distanceKm;
  private Integer activeCount;
  private Double performanceScore;
  private Double finalScore;
  private Integer chosen;
  private String reason;
  private LocalDateTime createdAt;
}

