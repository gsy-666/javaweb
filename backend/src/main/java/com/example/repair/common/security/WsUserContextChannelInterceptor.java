package com.example.repair.common.security;

import java.util.Map;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

public class WsUserContextChannelInterceptor implements ChannelInterceptor {

  public static final String ATTR_PRINCIPAL = WsAuthHandshakeInterceptor.ATTR_PRINCIPAL;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (acc == null) return message;

    if (StompCommand.SEND.equals(acc.getCommand()) || StompCommand.SUBSCRIBE.equals(acc.getCommand())) {
      UserContext.UserPrincipal p = getPrincipal(acc.getSessionAttributes());
      if (p != null) {
        UserContext.set(p);
      }
    }

    return message;
  }

  @Override
  public void afterSendCompletion(
      Message<?> message, MessageChannel channel, boolean sent, @Nullable Exception ex) {
    UserContext.clear();
  }

  private UserContext.UserPrincipal getPrincipal(@Nullable Map<String, Object> attrs) {
    if (attrs == null) return null;
    Object v = attrs.get(ATTR_PRINCIPAL);
    if (v instanceof UserContext.UserPrincipal p) return p;
    return null;
  }
}
