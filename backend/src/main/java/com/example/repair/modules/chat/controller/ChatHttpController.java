package com.example.repair.modules.chat.controller;

import com.example.repair.common.api.ApiResponse;
import com.example.repair.modules.chat.entity.ChatMessage;
import com.example.repair.modules.chat.service.ChatService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/work-orders/{workOrderId}/chat")
@RequiredArgsConstructor
public class ChatHttpController {
  private final ChatService chatService;

  @GetMapping("/messages")
  public ApiResponse<List<ChatMessage>> list(
      @PathVariable Long workOrderId, @RequestParam(defaultValue = "200") int limit) {
    return ApiResponse.ok(chatService.list(workOrderId, limit));
  }
}

