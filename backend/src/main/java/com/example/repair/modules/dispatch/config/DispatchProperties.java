package com.example.repair.modules.dispatch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.dispatch")
public class DispatchProperties {
  private int candidateLimit = 20;
  private int inviteTopN = 3;
  private int locationExpireSeconds = 600;
  private int maxActiveOrders = 5;
  private int cooldownSeconds = 300;
  private double wDistance = 0.55;
  private double wLoad = 0.35;
  private double wPerformance = 0.10;
}

