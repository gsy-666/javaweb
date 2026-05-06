package com.example.repair.modules.worker.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.repair.common.api.ApiResponse;
import com.example.repair.common.security.UserContext;
import com.example.repair.modules.dispatch.config.DispatchProperties;
import com.example.repair.modules.worker.dto.WorkerLocationRequest;
import com.example.repair.modules.worker.entity.WorkerProfile;
import com.example.repair.modules.worker.mapper.WorkerProfileMapper;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
public class WorkerController {
  private final WorkerProfileMapper workerProfileMapper;
  private final StringRedisTemplate redis;
  private final DispatchProperties dispatchProperties;

  @PostMapping("/me/location")
  public ApiResponse<Void> updateMyLocation(@Valid @RequestBody WorkerLocationRequest req) {
    Long workerId = UserContext.getRequired().getUserId();
    LocalDateTime now = LocalDateTime.now();

    WorkerProfile existed =
        workerProfileMapper.selectOne(new LambdaQueryWrapper<WorkerProfile>().eq(WorkerProfile::getUserId, workerId));
    if (existed == null) {
      existed = new WorkerProfile();
      existed.setUserId(workerId);
      existed.setTradeCode(req.getTradeCode());
      existed.setAcceptOrders(Boolean.TRUE.equals(req.getAcceptOrders()) ? 1 : 0);
      existed.setLastLng(req.getLng());
      existed.setLastLat(req.getLat());
      existed.setLastLocationAt(now);
      existed.setCreatedAt(now);
      existed.setUpdatedAt(now);
      workerProfileMapper.insert(existed);
    } else {
      existed.setTradeCode(req.getTradeCode());
      existed.setAcceptOrders(Boolean.TRUE.equals(req.getAcceptOrders()) ? 1 : 0);
      existed.setLastLng(req.getLng());
      existed.setLastLat(req.getLat());
      existed.setLastLocationAt(now);
      existed.setUpdatedAt(now);
      workerProfileMapper.updateById(existed);
    }

    String geoKey = "worker:geo:" + req.getTradeCode();
    String member = String.valueOf(workerId);
    if (Boolean.TRUE.equals(req.getAcceptOrders())) {
      redis.opsForGeo().add(geoKey, new Point(req.getLng(), req.getLat()), member);
      redis
          .opsForValue()
          .set(
              "worker:online:" + workerId,
              "1",
              Duration.ofSeconds(Math.max(30, dispatchProperties.getLocationExpireSeconds())));
    } else {
      redis.delete("worker:online:" + workerId);
      redis.opsForGeo().remove(geoKey, member);
    }
    return ApiResponse.ok();
  }
}

