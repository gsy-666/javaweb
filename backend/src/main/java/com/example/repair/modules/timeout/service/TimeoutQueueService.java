package com.example.repair.modules.timeout.service;

import com.example.repair.modules.timeout.config.SlaProperties;
import com.example.repair.modules.timeout.model.TimeoutStage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimeoutQueueService {
  private final StringRedisTemplate redis;
  private final SlaProperties sla;

  @Value("${app.timeout.enabled:true}")
  private boolean enabled;

  private static final String KEY_DELAY = "wo:delay";

  public void scheduleAssign(Long workOrderId, LocalDateTime base) {
    if (!enabled) return;
    schedule(workOrderId, TimeoutStage.ASSIGN, base.plusSeconds(Math.max(1, sla.getAssignTimeoutSeconds())));
  }

  public void scheduleAccept(Long workOrderId, LocalDateTime base) {
    if (!enabled) return;
    schedule(workOrderId, TimeoutStage.ACCEPT, base.plusSeconds(Math.max(1, sla.getAcceptTimeoutSeconds())));
  }

  public void scheduleFirstUpdate(Long workOrderId, LocalDateTime base) {
    if (!enabled) return;
    schedule(workOrderId, TimeoutStage.FIRST_UPDATE, base.plusSeconds(Math.max(1, sla.getFirstUpdateTimeoutSeconds())));
  }

  public void scheduleFinish(Long workOrderId, LocalDateTime base) {
    if (!enabled) return;
    schedule(workOrderId, TimeoutStage.FINISH, base.plusSeconds(Math.max(1, sla.getFinishTimeoutSeconds())));
  }

  public void cancel(Long workOrderId, TimeoutStage stage) {
    if (!enabled) return;
    redis.opsForZSet().remove(KEY_DELAY, member(workOrderId, stage));
  }

  public void cancelAll(Long workOrderId) {
    if (!enabled) return;
    for (TimeoutStage s : TimeoutStage.values()) {
      cancel(workOrderId, s);
    }
  }

  public String member(Long workOrderId, TimeoutStage stage) {
    return workOrderId + ":" + stage.name();
  }

  private void schedule(Long workOrderId, TimeoutStage stage, LocalDateTime deadline) {
    long epochMs = deadline.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    redis.opsForZSet().add(KEY_DELAY, member(workOrderId, stage), epochMs);
  }

  public long nowMs() {
    return Instant.now().toEpochMilli();
  }

  public String keyDelay() {
    return KEY_DELAY;
  }
}

