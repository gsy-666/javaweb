package com.example.repair.modules.timeout.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("work_order_escalation")
public class WorkOrderEscalation {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long workOrderId;
  private Integer level;
  private String stage;
  private String action;
  private String note;
  private LocalDateTime createdAt;
}

