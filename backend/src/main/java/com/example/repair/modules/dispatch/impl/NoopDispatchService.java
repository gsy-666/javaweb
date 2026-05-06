package com.example.repair.modules.dispatch.impl;

import com.example.repair.modules.dispatch.DispatchService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.dispatch.enabled", havingValue = "false", matchIfMissing = true)
public class NoopDispatchService implements DispatchService {
  @Override
  public void triggerDispatch(Long workOrderId) {}

  @Override
  public void cooldownWorker(Long workerId) {}

  @Override
  public void decreaseActiveCount(Long workerId) {}
}

