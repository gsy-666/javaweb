package com.example.repair.modules.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("chat_thread")
public class ChatThread {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long workOrderId;
  private LocalDateTime createdAt;
}

