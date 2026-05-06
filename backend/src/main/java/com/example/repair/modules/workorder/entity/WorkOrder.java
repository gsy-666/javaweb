package com.example.repair.modules.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("work_order")
public class WorkOrder {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String code;
  private Long requesterId;
  private String tradeCode;
  private String description;

  private Long buildingId;
  private Long areaId;
  private String address;
  private Double lng;
  private Double lat;

  private String status;
  private Integer priority;
  private Long assignedWorkerId;
  private Integer escalationLevel;

  private LocalDateTime createdAt;
  private LocalDateTime assignedAt;
  private LocalDateTime acceptedAt;
  private LocalDateTime etaAt;
  private LocalDateTime finishedAt;
  private LocalDateTime closedAt;
  private LocalDateTime cancelledAt;
  private LocalDateTime deletedAt;
}

