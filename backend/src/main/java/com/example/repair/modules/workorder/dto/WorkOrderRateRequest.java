package com.example.repair.modules.workorder.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class WorkOrderRateRequest {
  @Min(1)
  @Max(5)
  private int stars;

  private List<String> tags;

  @Size(max = 1000)
  private String comment;
}

