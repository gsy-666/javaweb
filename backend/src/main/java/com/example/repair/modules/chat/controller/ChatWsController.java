package com.example.repair.modules.chat.controller;

import com.example.repair.common.security.UserContext;
import com.example.repair.modules.chat.dto.ChatSendRequest;
import com.example.repair.modules.chat.entity.ChatMessage;
import com.example.repair.modules.chat.service.ChatService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWsController {
  private final ChatService chatService;
  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/wo/{workOrderId}/send")
  public void send(
      @DestinationVariable Long workOrderId,
      @Valid @Payload ChatSendRequest req,
      @Headers Map<String, Object> headers) {
    Long senderId = resolveSenderId(headers);
    if (senderId == null) return;

    ChatMessage saved = chatService.send(workOrderId, senderId, req);
    messagingTemplate.convertAndSend("/topic/wo/" + workOrderId, saved);
  }

  private Long resolveSenderId(Map<String, Object> headers) {
    try {
      return UserContext.getRequired().getUserId();
    } catch (Exception ignore) {
      // ignore
    }

    if (headers == null) return null;

    Object sessionAttrs = headers.get("simpSessionAttributes");
    if (sessionAttrs instanceof Map<?, ?> m) {
      Object p = m.get("sr_principal");
      if (p instanceof UserContext.UserPrincipal up) {
        return up.getUserId();
      }
    }

    return null;
  }
}

