package com.example.repair.modules.kpi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.repair.common.api.ApiResponse;
import com.example.repair.modules.auth.entity.SysUser;
import com.example.repair.modules.auth.mapper.SysUserMapper;
import com.example.repair.modules.kpi.dto.KpiWorkerRow;
import com.example.repair.modules.kpi.entity.KpiDailyWorker;
import com.example.repair.modules.kpi.mapper.KpiDailyWorkerMapper;
import com.example.repair.modules.kpi.service.KpiAggregationService;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kpi")
@RequiredArgsConstructor
public class KpiController {
  private final KpiDailyWorkerMapper dailyMapper;
  private final KpiAggregationService aggregationService;
  private final SysUserMapper sysUserMapper;

  @GetMapping("/workers")
  public ApiResponse<List<KpiWorkerRow>> workers(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    LocalDate d = date == null ? LocalDate.now() : date;
    aggregationService.aggregateDaily(d);

    List<KpiDailyWorker> list =
        dailyMapper.selectList(
            new LambdaQueryWrapper<KpiDailyWorker>()
                .eq(KpiDailyWorker::getStatDate, d)
                .orderByDesc(KpiDailyWorker::getKpiScore));

    List<Long> workerIds =
        list.stream().map(KpiDailyWorker::getWorkerId).filter(Objects::nonNull).distinct().toList();

    Map<Long, SysUser> userById = new HashMap<>();
    if (!workerIds.isEmpty()) {
      userById =
          sysUserMapper.selectBatchIds(workerIds).stream()
              .collect(Collectors.toMap(SysUser::getId, u2 -> u2));
    }

    final Map<Long, SysUser> userByIdFinal = userById;

    List<KpiWorkerRow> rows =
        list.stream()
            .map(
                r -> {
                  SysUser u = userByIdFinal.get(r.getWorkerId());
                  return new KpiWorkerRow(
                      r.getWorkerId(),
                      u == null ? null : u.getDisplayName(),
                      u == null ? null : u.getUsername(),
                      u == null ? null : u.getPhone(),
                      r.getDoneCnt(),
                      r.getAvgAcceptMin(),
                      r.getAvgFirstUpdateMin(),
                      r.getSlaRate(),
                      r.getAvgRating(),
                      r.getBadRate(),
                      r.getKpiScore());
                })
            .toList();

    return ApiResponse.ok(rows);
  }
}

