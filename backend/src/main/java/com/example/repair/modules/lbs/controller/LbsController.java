package com.example.repair.modules.lbs.controller;

import com.example.repair.common.api.ApiResponse;
import com.example.repair.modules.lbs.service.LbsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lbs")
@RequiredArgsConstructor
public class LbsController {
  private final LbsService lbsService;

  @GetMapping("/regeo")
  public ApiResponse<String> regeo(@RequestParam double lng, @RequestParam double lat) {
    return ApiResponse.ok(lbsService.reverseGeocode(lng, lat));
  }
}

