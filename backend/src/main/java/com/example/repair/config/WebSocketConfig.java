package com.example.repair.config;

import com.example.repair.common.security.TokenAuthService;
import com.example.repair.common.security.WsAuthHandshakeInterceptor;
import com.example.repair.common.security.WsUserContextChannelInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final TokenAuthService tokenAuthService;

  public WebSocketConfig(
      StringRedisTemplate redis,
      @Value("${app.auth.allow-mock-token:false}") boolean allowMockToken) {
    this.tokenAuthService = new TokenAuthService(redis, allowMockToken);
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic");
    registry.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new WsUserContextChannelInterceptor());
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/ws")
        .addInterceptors(new WsAuthHandshakeInterceptor(tokenAuthService))
        .setAllowedOriginPatterns("*")
        .withSockJS();
  }
}

