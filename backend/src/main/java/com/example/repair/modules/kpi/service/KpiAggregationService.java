package com.example.repair.modules.kpi.service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KpiAggregationService {
  private final JdbcTemplate jdbc;

  @Transactional
  public void aggregateDaily(LocalDate statDate) {
    LocalDateTime start = statDate.atStartOfDay();
    LocalDateTime end = statDate.plusDays(1).atStartOfDay();

    String sql =
        """
        SELECT
          wo.assigned_worker_id AS worker_id,
          COUNT(*) AS done_cnt,
          AVG(TIMESTAMPDIFF(SECOND, wo.assigned_at, wo.accepted_at))/60.0 AS avg_accept_min,
          AVG(r.stars) AS avg_rating,
          SUM(CASE WHEN r.stars <= 2 THEN 1 ELSE 0 END) / NULLIF(COUNT(r.id), 0) AS bad_rate
        FROM work_order wo
        LEFT JOIN work_order_rating r ON r.work_order_id = wo.id
        WHERE wo.closed_at >= ? AND wo.closed_at < ?
          AND wo.assigned_worker_id IS NOT NULL
          AND wo.status = 'CLOSED'
        GROUP BY wo.assigned_worker_id
        """;

    List<Map<String, Object>> rows = jdbc.queryForList(sql, start, end);
    for (Map<String, Object> row : rows) {
      Long workerId = ((Number) row.get("worker_id")).longValue();
      int doneCnt = ((Number) row.get("done_cnt")).intValue();
      Double avgAcceptMin = row.get("avg_accept_min") == null ? null : ((Number) row.get("avg_accept_min")).doubleValue();
      Double avgRating = row.get("avg_rating") == null ? null : ((Number) row.get("avg_rating")).doubleValue();
      Double badRate = row.get("bad_rate") == null ? 0.0 : ((Number) row.get("bad_rate")).doubleValue();

      double score = score(doneCnt, avgAcceptMin, avgRating, badRate);

      jdbc.update(
          """
          INSERT INTO kpi_daily_worker(stat_date, worker_id, done_cnt, avg_accept_min, avg_rating, bad_rate, kpi_score, created_at, updated_at)
          VALUES(?,?,?,?,?,?,?,NOW(),NOW())
          ON DUPLICATE KEY UPDATE
            done_cnt=VALUES(done_cnt),
            avg_accept_min=VALUES(avg_accept_min),
            avg_rating=VALUES(avg_rating),
            bad_rate=VALUES(bad_rate),
            kpi_score=VALUES(kpi_score),
            updated_at=NOW()
          """,
          Date.valueOf(statDate),
          workerId,
          doneCnt,
          avgAcceptMin,
          avgRating,
          badRate,
          score);
    }
    log.info("Daily KPI aggregated: date={} rows={}", statDate, rows.size());
  }

  @Transactional
  public void aggregateMonthly(String statMonth) {
    // statMonth: yyyy-MM
    LocalDate firstDay = LocalDate.parse(statMonth + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    LocalDateTime start = firstDay.atStartOfDay();
    LocalDateTime end = firstDay.plusMonths(1).atStartOfDay();

    String sql =
        """
        SELECT
          wo.assigned_worker_id AS worker_id,
          COUNT(*) AS done_cnt,
          AVG(TIMESTAMPDIFF(SECOND, wo.assigned_at, wo.accepted_at))/60.0 AS avg_accept_min,
          AVG(r.stars) AS avg_rating,
          SUM(CASE WHEN r.stars <= 2 THEN 1 ELSE 0 END) / NULLIF(COUNT(r.id), 0) AS bad_rate
        FROM work_order wo
        LEFT JOIN work_order_rating r ON r.work_order_id = wo.id
        WHERE wo.closed_at >= ? AND wo.closed_at < ?
          AND wo.assigned_worker_id IS NOT NULL
          AND wo.status = 'CLOSED'
        GROUP BY wo.assigned_worker_id
        """;

    List<Map<String, Object>> rows = jdbc.queryForList(sql, start, end);
    for (Map<String, Object> row : rows) {
      Long workerId = ((Number) row.get("worker_id")).longValue();
      int doneCnt = ((Number) row.get("done_cnt")).intValue();
      Double avgAcceptMin = row.get("avg_accept_min") == null ? null : ((Number) row.get("avg_accept_min")).doubleValue();
      Double avgRating = row.get("avg_rating") == null ? null : ((Number) row.get("avg_rating")).doubleValue();
      Double badRate = row.get("bad_rate") == null ? 0.0 : ((Number) row.get("bad_rate")).doubleValue();
      double score = score(doneCnt, avgAcceptMin, avgRating, badRate);

      jdbc.update(
          """
          INSERT INTO kpi_monthly_worker(stat_month, worker_id, done_cnt, avg_accept_min, avg_rating, bad_rate, kpi_score, created_at, updated_at)
          VALUES(?,?,?,?,?,?,?,NOW(),NOW())
          ON DUPLICATE KEY UPDATE
            done_cnt=VALUES(done_cnt),
            avg_accept_min=VALUES(avg_accept_min),
            avg_rating=VALUES(avg_rating),
            bad_rate=VALUES(bad_rate),
            kpi_score=VALUES(kpi_score),
            updated_at=NOW()
          """,
          statMonth,
          workerId,
          doneCnt,
          avgAcceptMin,
          avgRating,
          badRate,
          score);
    }
    log.info("Monthly KPI aggregated: month={} rows={}", statMonth, rows.size());
  }

  private double score(int doneCnt, Double avgAcceptMin, Double avgRating, Double badRate) {
    double rating = avgRating == null ? 0.0 : avgRating;
    double accept = avgAcceptMin == null ? 60.0 : avgAcceptMin;
    double bad = badRate == null ? 0.0 : badRate;
    // interpretable baseline scoring
    return doneCnt * 10.0 + rating * 20.0 - bad * 50.0 - accept * 0.1;
  }
}

