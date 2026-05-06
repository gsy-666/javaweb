package com.example.repair.modules.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("work_order_rating")
public class WorkOrderRating {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long workOrderId;
  private Long raterId;
  private Integer stars;
  private String tags;
  private String comment;
  private LocalDateTime createdAt;
}

