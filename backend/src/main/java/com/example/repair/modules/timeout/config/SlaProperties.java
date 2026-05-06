package com.example.repair.modules.timeout.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.sla")
public class SlaProperties {
  private int assignTimeoutSeconds = 60;
  private int acceptTimeoutSeconds = 300;
  private int firstUpdateTimeoutSeconds = 900;
  private int finishTimeoutSeconds = 7200;
}

