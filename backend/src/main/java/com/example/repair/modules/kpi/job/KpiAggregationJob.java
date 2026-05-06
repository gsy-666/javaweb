package com.example.repair.modules.kpi.job;

import com.example.repair.modules.kpi.service.KpiAggregationService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KpiAggregationJob {
  private final KpiAggregationService aggregationService;

  @Scheduled(cron = "0 5 1 * * *")
  public void daily() {
    LocalDate d = LocalDate.now().minusDays(1);
    aggregationService.aggregateDaily(d);
    aggregationService.aggregateMonthly(d.format(DateTimeFormatter.ofPattern("yyyy-MM")));
    log.info("KPI aggregation done for {}", d);
  }
}

