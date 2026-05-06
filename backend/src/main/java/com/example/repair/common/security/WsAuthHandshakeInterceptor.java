package com.example.repair.common.security;

import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

public class WsAuthHandshakeInterceptor implements HandshakeInterceptor {

  public static final String ATTR_PRINCIPAL = "sr_principal";

  private final TokenAuthService tokenAuthService;

  public WsAuthHandshakeInterceptor(TokenAuthService tokenAuthService) {
    this.tokenAuthService = tokenAuthService;
  }

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {
    String token = tokenAuthService.extractToken(request);
    if (token == null || token.isBlank()) return false;

    UserContext.UserPrincipal p = tokenAuthService.authenticate(token);
    if (p == null) return false;

    attributes.put(ATTR_PRINCIPAL, p);
    return true;
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      @Nullable Exception exception) {
    // no-op
  }
}
