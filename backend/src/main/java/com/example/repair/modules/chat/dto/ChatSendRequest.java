package com.example.repair.modules.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatSendRequest {
  @NotBlank private String msgType;

  @NotBlank
  @Size(max = 2000)
  private String content;
}

