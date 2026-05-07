package com.example.repair.modules.dispatch.impl;

import com.example.repair.modules.dispatch.DispatchService;
import com.example.repair.modules.dispatch.config.DispatchProperties;
import com.example.repair.modules.dispatch.entity.WorkOrderAssignment;
import com.example.repair.modules.dispatch.mapper.WorkOrderAssignmentMapper;
import com.example.repair.modules.timeout.model.TimeoutStage;
import com.example.repair.modules.timeout.service.TimeoutQueueService;
import com.example.repair.modules.workorder.entity.WorkOrder;
import com.example.repair.modules.workorder.entity.WorkOrderProgress;
import com.example.repair.modules.workorder.mapper.WorkOrderMapper;
import com.example.repair.modules.workorder.mapper.WorkOrderProgressMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Primary
@Service
@ConditionalOnProperty(name = "app.dispatch.enabled", havingValue = "true")
@RequiredArgsConstructor
public class RedisGeoDispatchService implements DispatchService {
  private final StringRedisTemplate redis;
  private final DispatchProperties props;
  private final WorkOrderMapper workOrderMapper;
  private final WorkOrderProgressMapper progressMapper;
  private final WorkOrderAssignmentMapper assignmentMapper;
  private final TimeoutQueueService timeoutQueueService;

  private static final String KEY_ACTIVE_COUNT = "worker:active_count";

  @Override
  public void decreaseActiveCount(Long workerId) {
    if (workerId == null) return;
    redis.opsForZSet().incrementScore(KEY_ACTIVE_COUNT, String.valueOf(workerId), -1.0);
  }

  @Override
  @Transactional
  public void triggerDispatch(Long workOrderId) {
    String lockKey = "lock:dispatch:{" + workOrderId + "}";
    Boolean locked =
        redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(10));
    if (!Boolean.TRUE.equals(locked)) {
      return;
    }
    try {
      WorkOrder wo = workOrderMapper.selectById(workOrderId);
      if (wo == null) return;
      if (!"NEW".equals(wo.getStatus())) return;
      if (wo.getLng() == null || wo.getLat() == null) {
        log.warn("Missing location for dispatch. workOrderId={}", workOrderId);
        return;
      }

      List<Candidate> candidates;
      try {
        candidates = findCandidates(wo.getTradeCode(), wo.getLng(), wo.getLat());
        if (candidates.isEmpty()) {
          candidates = findCandidatesFallbackAllTrades(wo.getLng(), wo.getLat());
        }
      } catch (Exception e) {
        log.warn("Dispatch skipped due to redis error. workOrderId={}", workOrderId, e);
        insertProgress(workOrderId, "NEW", "NEW", "系统派单：派单系统暂不可用");
        return;
      }
      if (candidates.isEmpty()) {
        insertProgress(workOrderId, "NEW", "NEW", "系统派单：未找到可用维修人员");
        return;
      }

      Candidate chosen =
          candidates.stream().max(Comparator.comparingDouble(Candidate::getFinalScore)).orElse(null);
      if (chosen == null) return;

      LocalDateTime now = LocalDateTime.now();
      // write audit rows
      for (Candidate c : candidates) {
        WorkOrderAssignment a = new WorkOrderAssignment();
        a.setWorkOrderId(workOrderId);
        a.setWorkerId(c.workerId);
        a.setDistanceKm(c.distanceKm);
        a.setActiveCount(c.activeCount);
        a.setPerformanceScore(c.performanceScore);
        a.setFinalScore(c.finalScore);
        a.setChosen(Objects.equals(c.workerId, chosen.workerId) ? 1 : 0);
        a.setReason(null);
        a.setCreatedAt(now);
        assignmentMapper.insert(a);
      }

      wo.setAssignedWorkerId(chosen.workerId);
      wo.setAssignedAt(now);
      wo.setStatus("ASSIGNED");
      workOrderMapper.updateById(wo);

      // update load
      redis.opsForZSet().incrementScore(KEY_ACTIVE_COUNT, String.valueOf(chosen.workerId), 1.0);

      insertProgress(workOrderId, "NEW", "ASSIGNED", "系统派单：已指派给维修人员#" + chosen.workerId);
      timeoutQueueService.cancel(workOrderId, TimeoutStage.ASSIGN);
      timeoutQueueService.scheduleAccept(workOrderId, now);
    } finally {
      redis.delete(lockKey);
    }
  }

  private List<Candidate> findCandidates(String tradeCode, double lng, double lat) {
    String geoKey = "worker:geo:" + tradeCode;
    List<Double> radiusesKm = List.of(1.0, 2.0, 5.0, 10.0, 20.0);
    List<Candidate> out = new ArrayList<>();

    for (double r : radiusesKm) {
      if (out.size() >= props.getCandidateLimit()) break;

      var results =
          redis
              .opsForGeo()
              .radius(
                  geoKey,
                  new Circle(new Point(lng, lat), new Distance(r, Metrics.KILOMETERS)));
      if (results == null) continue;

      results.forEach(
          gr -> {
            String member = gr.getContent().getName();
            if (member == null) return;
            Long workerId;
            try {
              workerId = Long.parseLong(member);
            } catch (Exception e) {
              return;
            }
            if (Boolean.TRUE.equals(redis.hasKey(cooldownKey(workerId)))) return;
            if (!Boolean.TRUE.equals(redis.hasKey(onlineKey(workerId)))) return;

            Double distKm = gr.getDistance() == null ? null : gr.getDistance().getValue();
            int activeCount = getActiveCount(workerId);
            if (activeCount >= props.getMaxActiveOrders()) return;

            double performanceScore = 0.5;
            double score =
                props.getWDistance() * (1.0 / (1.0 + (distKm == null ? 999 : distKm)))
                    + props.getWLoad() * (1.0 / (1.0 + activeCount))
                    + props.getWPerformance() * performanceScore;

            out.add(new Candidate(workerId, distKm == null ? 999 : distKm, activeCount, performanceScore, score));
          });
    }

    // de-dup by workerId keeping best score
    return out.stream()
        .collect(
            java.util.stream.Collectors.toMap(
                c -> c.workerId, c -> c, (a, b) -> a.finalScore >= b.finalScore ? a : b))
        .values()
        .stream()
        .sorted(Comparator.comparingDouble(Candidate::getFinalScore).reversed())
        .limit(props.getCandidateLimit())
        .toList();
  }


  private List<Candidate> findCandidatesFallbackAllTrades(double lng, double lat) {
    List<String> keys = redis.keys("worker:geo:*") == null ? List.of() : redis.keys("worker:geo:*").stream().toList();
    if (keys.isEmpty()) return List.of();

    List<Candidate> out = new ArrayList<>();
    List<Double> radiusesKm = List.of(1.0, 2.0, 5.0, 10.0, 20.0);

    for (String geoKey : keys) {
      for (double r : radiusesKm) {
        if (out.size() >= props.getCandidateLimit()) break;

        var results =
            redis
                .opsForGeo()
                .radius(
                    geoKey,
                    new Circle(new Point(lng, lat), new Distance(r, Metrics.KILOMETERS)));
        if (results == null) continue;

        results.forEach(
            gr -> {
              String member = gr.getContent().getName();
              if (member == null) return;
              Long workerId;
              try {
                workerId = Long.parseLong(member);
              } catch (Exception e) {
                return;
              }
              if (Boolean.TRUE.equals(redis.hasKey(cooldownKey(workerId)))) return;
              if (!Boolean.TRUE.equals(redis.hasKey(onlineKey(workerId)))) return;

              Double distKm = gr.getDistance() == null ? null : gr.getDistance().getValue();
              int activeCount = getActiveCount(workerId);
              if (activeCount >= props.getMaxActiveOrders()) return;

              double performanceScore = 0.5;
              double score =
                  props.getWDistance() * (1.0 / (1.0 + (distKm == null ? 999 : distKm)))
                      + props.getWLoad() * (1.0 / (1.0 + activeCount))
                      + props.getWPerformance() * performanceScore;

              out.add(
                  new Candidate(
                      workerId, distKm == null ? 999 : distKm, activeCount, performanceScore, score));
            });
      }
    }

    return out.stream()
        .collect(
            java.util.stream.Collectors.toMap(
                c -> c.workerId, c -> c, (a, b) -> a.finalScore >= b.finalScore ? a : b))
        .values()
        .stream()
        .sorted(Comparator.comparingDouble(Candidate::getFinalScore).reversed())
        .limit(props.getCandidateLimit())
        .toList();
  }

  private int getActiveCount(Long workerId) {
    Double s = redis.opsForZSet().score(KEY_ACTIVE_COUNT, String.valueOf(workerId));
    if (s == null) return 0;
    return (int) Math.round(s);
  }

  private void insertProgress(Long workOrderId, String from, String to, String msg) {
    WorkOrderProgress p = new WorkOrderProgress();
    p.setWorkOrderId(workOrderId);
    p.setFromStatus(from);
    p.setToStatus(to);
    p.setMessage(msg);
    p.setOperatorId(null);
    p.setCreatedAt(LocalDateTime.now());
    progressMapper.insert(p);
  }

  private String cooldownKey(Long workerId) {
    return "worker:cooldown:" + workerId;
  }

  private String onlineKey(Long workerId) {
    return "worker:online:" + workerId;
  }

  @Override
  public void cooldownWorker(Long workerId) {
    if (workerId == null) return;
    redis
        .opsForValue()
        .set(cooldownKey(workerId), "1", Duration.ofSeconds(props.getCooldownSeconds()));
  }

  @Value
  private static class Candidate {
    Long workerId;
    double distanceKm;
    int activeCount;
    double performanceScore;
    double finalScore;
  }
}

