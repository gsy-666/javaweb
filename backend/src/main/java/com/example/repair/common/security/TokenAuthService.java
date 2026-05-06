package com.example.repair.common.security;

import java.net.URI;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

public class TokenAuthService {
  private final StringRedisTemplate redis;
  private final boolean allowMockToken;

  public TokenAuthService(StringRedisTemplate redis, boolean allowMockToken) {
    this.redis = redis;
    this.allowMockToken = allowMockToken;
  }

  public String extractToken(ServerHttpRequest request) {
    if (request == null) return null;

    List<String> auths = request.getHeaders().get("Authorization");
    if (auths != null) {
      for (String auth : auths) {
        if (auth != null && auth.startsWith("Bearer ")) {
          String t = auth.substring("Bearer ".length()).trim();
          if (!t.isBlank()) return t;
        }
      }
    }

    URI uri = request.getURI();
    if (uri != null) {
      MultiValueMap<String, String> q = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
      String t = q.getFirst("token");
      if (t != null && !t.isBlank()) return t.trim();
    }

    return null;
  }

  public UserContext.UserPrincipal authenticate(String token) {
    if (token == null || token.isBlank()) return null;

    if (allowMockToken && token.startsWith("mock:")) {
      return parseMockToken(token);
    }

    String value;
    try {
      value = redis.opsForValue().get(tokenKey(token));
    } catch (Exception ignored) {
      value = null;
    }

    if (value == null || value.isBlank()) return null;

    String[] parts = value.split("\\|", 3);
    if (parts.length < 2) return null;

    Long userId = parsePositiveLong(parts[0]);
    String role = parts[1];
    if (userId == null) return null;

    return new UserContext.UserPrincipal(userId, role);
  }

  private String tokenKey(String token) {
    return "sr:token:" + token;
  }

  private Long parsePositiveLong(String s) {
    try {
      long v = Long.parseLong(s);
      return v > 0 ? v : null;
    } catch (Exception e) {
      return null;
    }
  }

  private UserContext.UserPrincipal parseMockToken(String token) {
    // mock:<userId>:<role>
    String[] parts = token.split(":", 3);
    if (parts.length < 3) return null;
    Long userId = parsePositiveLong(parts[1]);
    if (userId == null) return null;
    String role = parts[2];
    if (role == null || role.isBlank()) return null;
    return new UserContext.UserPrincipal(userId, role);
  }
}
