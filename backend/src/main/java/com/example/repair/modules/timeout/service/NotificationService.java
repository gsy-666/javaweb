package com.example.repair.modules.timeout.service;

public interface NotificationService {
  void notifyWorker(Long workerId, String message);

  void notifyAdmin(String message);
}

