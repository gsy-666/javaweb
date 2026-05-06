package com.example.repair.modules.kpi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("kpi_monthly_worker")
public class KpiMonthlyWorker {
  @TableId(type = IdType.AUTO)
  private Long id;
  private String statMonth;
  private Long workerId;
  private Integer doneCnt;
  private Double avgAcceptMin;
  private Double avgFirstUpdateMin;
  private Double slaRate;
  private Double avgRating;
  private Double badRate;
  private Double kpiScore;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

