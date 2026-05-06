package com.example.repair.modules.dispatch;

public interface DispatchService {
  void triggerDispatch(Long workOrderId);

  void cooldownWorker(Long workerId);

  void decreaseActiveCount(Long workerId);
}

