package com.example.repair.modules.lbs.service.impl;

import com.example.repair.modules.lbs.service.LbsService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(LbsService.class)
public class NoopLbsService implements LbsService {
  @Override
  public String reverseGeocode(double lng, double lat) {
    return null;
  }
}

