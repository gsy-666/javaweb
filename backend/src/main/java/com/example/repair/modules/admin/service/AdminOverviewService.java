package com.example.repair.modules.admin.service;

import com.example.repair.modules.admin.dto.AdminOverviewResponse;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminOverviewService {
  private final JdbcTemplate jdbc;

  public AdminOverviewResponse load() {
    LocalDateTime start = LocalDate.now().atStartOfDay();
    LocalDateTime end = start.plusDays(1);

    long total = queryLong("select count(*) from work_order");
    long todayReported = queryLong("select count(*) from work_order where created_at >= ? and created_at < ?", start, end);

    Map<String, Long> statusCounts = queryCountMap("select status as k, count(*) as v from work_order group by status");
    Map<String, Long> tradeCounts = queryCountMap("select trade_code as k, count(*) as v from work_order group by trade_code");

    long overdue = queryLong("select count(*) from work_order where escalation_level > 0");
    long pending = queryLong("select count(*) from work_order where status in ('NEW','ASSIGNED','ACCEPTED','IN_PROGRESS','WAIT_USER','DONE_WAIT_RATE')");
    long done = queryLong("select count(*) from work_order where status in ('DONE_WAIT_RATE','CLOSED')");

    Double avgStarsObj = jdbc.queryForObject("select avg(stars) from work_order_rating", Double.class);
    double avgStars = avgStarsObj == null ? 0.0 : avgStarsObj;

    Map<Integer, Long> starsCounts = new LinkedHashMap<>();
    for (int s = 5; s >= 1; s--) starsCounts.put(s, 0L);
    jdbc.query(
        "select stars as k, count(*) as v from work_order_rating group by stars",
        rs -> {
          int k = rs.getInt("k");
          long v = rs.getLong("v");
          if (starsCounts.containsKey(k)) starsCounts.put(k, v);
        });

    List<AdminOverviewResponse.RecentRating> recentRatings =
        jdbc.query(
            "select work_order_id, stars, comment, tags, created_at from work_order_rating order by created_at desc limit 8",
            (rs, i) -> {
              Timestamp ts = rs.getTimestamp("created_at");
              String createdAt = ts == null ? null : ts.toLocalDateTime().toString();
              return new AdminOverviewResponse.RecentRating(
                  rs.getLong("work_order_id"),
                  rs.getInt("stars"),
                  rs.getString("comment"),
                  rs.getString("tags"),
                  createdAt);
            });

    return new AdminOverviewResponse(
        total,
        todayReported,
        overdue,
        pending,
        done,
        statusCounts,
        tradeCounts,
        avgStars,
        starsCounts,
        recentRatings);
  }

  private long queryLong(String sql, Object... args) {
    Long v = jdbc.queryForObject(sql, Long.class, args);
    return v == null ? 0L : v;
  }

  private Map<String, Long> queryCountMap(String sql, Object... args) {
    Map<String, Long> out = new LinkedHashMap<>();
    List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
    for (Map<String, Object> row : rows) {
      Object kObj = row.get("k");
      Object vObj = row.get("v");
      String k = kObj == null ? "" : String.valueOf(kObj);
      long v = vObj == null ? 0L : ((Number) vObj).longValue();
      out.put(k, v);
    }
    return out;
  }
}
