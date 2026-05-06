package com.example.repair.modules.kpi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KpiWorkerRow {
  private Long workerId;
  private String workerName;
  private String workerAccount;
  private String workerPhone;

  private Integer doneCnt;
  private Double avgAcceptMin;
  private Double avgFirstUpdateMin;
  private Double slaRate;
  private Double avgRating;
  private Double badRate;
  private Double kpiScore;
}
