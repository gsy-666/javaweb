package com.example.repair.modules.timeout.job;

import com.example.repair.modules.timeout.service.TimeoutQueueService;
import com.example.repair.modules.timeout.service.TimeoutStageProcessor;
import java.time.Duration;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeoutScannerJob {
  private final StringRedisTemplate redis;
  private final TimeoutQueueService queue;
  private final TimeoutStageProcessor processor;

  @Scheduled(fixedDelayString = "5000")
  public void scan() {
    long now = queue.nowMs();
    Set<String> due =
        redis.opsForZSet().rangeByScore(queue.keyDelay(), 0, now, 0, 50);
    if (due == null || due.isEmpty()) return;

    for (String member : due) {
      try {
        processor.processMember(member);
      } catch (Exception e) {
        log.warn("Process timeout member failed: {}", member, e);
      } finally {
        redis.opsForZSet().remove(queue.keyDelay(), member);
      }
    }
  }
}

