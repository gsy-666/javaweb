package com.example.repair.modules.timeout.service.impl;

import com.example.repair.modules.timeout.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LogNotificationService implements NotificationService {
  @Override
  public void notifyWorker(Long workerId, String message) {
    log.warn("[NotifyWorker] workerId={} msg={}", workerId, message);
  }

  @Override
  public void notifyAdmin(String message) {
    log.warn("[NotifyAdmin] msg={}", message);
  }
}

