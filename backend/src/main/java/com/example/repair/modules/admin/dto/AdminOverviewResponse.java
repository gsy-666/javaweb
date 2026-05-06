package com.example.repair.modules.admin.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOverviewResponse {
  private long total;
  private long todayReported;
  private long overdue;
  private long pending;
  private long done;

  private Map<String, Long> statusCounts;
  private Map<String, Long> tradeCounts;

  private double avgStars;
  private Map<Integer, Long> starsCounts;
  private List<RecentRating> recentRatings;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecentRating {
    private Long workOrderId;
    private Integer stars;
    private String comment;
    private String tags;
    private String createdAt;
  }
}
