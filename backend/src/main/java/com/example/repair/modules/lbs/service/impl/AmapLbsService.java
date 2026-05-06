package com.example.repair.modules.lbs.service.impl;

import com.example.repair.modules.lbs.service.LbsService;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.amap", name = "key")
public class AmapLbsService implements LbsService {
  private final StringRedisTemplate redis;

  private final RestClient restClient = RestClient.create();

  @Value("${app.amap.key:}")
  private String amapKey;

  @Value("${app.amap.geocode-url:https://restapi.amap.com/v3/geocode/regeo}")
  private String regeoUrl;

  @Override
  public String reverseGeocode(double lng, double lat) {
    String cacheKey = cacheKey(lng, lat);
    String cached = redis.opsForValue().get(cacheKey);
    if (cached != null) return cached;
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> resp =
          restClient
              .get()
              .uri(
                  regeoUrl
                      + "?key={key}&location={location}&extensions=base&radius=100&batch=false&roadlevel=0",
                  Map.of("key", amapKey, "location", lng + "," + lat))
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .body(Map.class);
      if (resp == null) return null;
      Object status = resp.get("status");
      if (!"1".equals(String.valueOf(status))) {
        log.warn("AMap regeo failed: {}", resp);
        return null;
      }
      Object regeocode = resp.get("regeocode");
      if (!(regeocode instanceof Map<?, ?> m)) return null;
      Object formatted = m.get("formatted_address");
      String addr = formatted == null ? null : String.valueOf(formatted);
      if (addr != null && !addr.isBlank()) {
        redis.opsForValue().set(cacheKey, addr, Duration.ofHours(24));
      }
      return addr;
    } catch (Exception e) {
      log.warn("AMap regeo exception", e);
      return null;
    }
  }

  private String cacheKey(double lng, double lat) {
    // reduce cardinality: 5 decimal ~ 1m, enough for address cache
    double rl = Math.round(lng * 100000d) / 100000d;
    double rt = Math.round(lat * 100000d) / 100000d;
    return "lbs:regeo:" + rl + ":" + rt;
  }
}

