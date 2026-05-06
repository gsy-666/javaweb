package com.example.repair.modules.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("work_order_progress")
public class WorkOrderProgress {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long workOrderId;
  private String fromStatus;
  private String toStatus;
  private String message;
  private Long operatorId;
  private LocalDateTime createdAt;
}

