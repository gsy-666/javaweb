package com.example.repair.modules.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.repair.modules.chat.dto.ChatSendRequest;
import com.example.repair.modules.chat.entity.ChatMessage;
import com.example.repair.modules.chat.entity.ChatThread;
import com.example.repair.modules.chat.mapper.ChatMessageMapper;
import com.example.repair.modules.chat.mapper.ChatThreadMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {
  private final ChatThreadMapper threadMapper;
  private final ChatMessageMapper messageMapper;
  private final ChatAuthService chatAuthService;

  @Transactional
  public ChatMessage send(Long workOrderId, Long senderId, ChatSendRequest req) {
    chatAuthService.requireCanAccessWorkOrder(workOrderId, senderId);

    ChatThread thread =
        threadMapper.selectOne(new LambdaQueryWrapper<ChatThread>().eq(ChatThread::getWorkOrderId, workOrderId));
    if (thread == null) {
      thread = new ChatThread();
      thread.setWorkOrderId(workOrderId);
      thread.setCreatedAt(LocalDateTime.now());
      threadMapper.insert(thread);
    }
    ChatMessage m = new ChatMessage();
    m.setThreadId(thread.getId());
    m.setWorkOrderId(workOrderId);
    m.setSenderId(senderId);
    m.setMsgType(req.getMsgType());
    m.setContent(req.getContent());
    m.setCreatedAt(LocalDateTime.now());
    messageMapper.insert(m);
    return m;
  }

  public List<ChatMessage> list(Long workOrderId, int limit) {
    chatAuthService.requireCanAccessWorkOrder(workOrderId);

    return messageMapper.selectList(
        new LambdaQueryWrapper<ChatMessage>()
            .eq(ChatMessage::getWorkOrderId, workOrderId)
            .orderByAsc(ChatMessage::getCreatedAt)
            .last("limit " + Math.max(1, Math.min(limit, 500))));
  }
}

