package com.example.repair.modules.workorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class WorkOrderCreateRequest {
  @NotBlank
  private String tradeCode;

  @NotBlank
  @Size(max = 1000)
  private String description;

  private Long buildingId;
  private Long areaId;

  private String address;

  @NotNull
  private Double lng;

  @NotNull
  private Double lat;

  private Integer priority = 0;

  private List<String> imageUrls;
}

