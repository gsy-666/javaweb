package com.example.repair.modules.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("work_order_image")
public class WorkOrderImage {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long workOrderId;
  private String kind;
  private String url;
  private LocalDateTime createdAt;
}

