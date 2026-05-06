package com.example.repair.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class UserContextFilter extends OncePerRequestFilter {

  private final StringRedisTemplate redis;

  @Value("${app.auth.allow-mock-token:false}")
  private boolean allowMockToken;

  public UserContextFilter(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      if (HttpMethod.OPTIONS.matches(request.getMethod())) {
        filterChain.doFilter(request, response);
        return;
      }

      if (isPublicPath(request.getRequestURI())) {
        filterChain.doFilter(request, response);
        return;
      }

      String auth = request.getHeader("Authorization");
      String token = null;
      if (auth != null && auth.startsWith("Bearer ")) {
        token = auth.substring("Bearer ".length()).trim();
      }

      if (token != null && !token.isBlank()) {
        if (allowMockToken && token.startsWith("mock:")) {
          UserContext.UserPrincipal p = parseMockToken(token);
          if (p != null) {
            UserContext.set(p);
            filterChain.doFilter(request, response);
            return;
          }
        }

        String value;
        try {
          value = redis.opsForValue().get(tokenKey(token));
        } catch (Exception ignored) {
          value = null;
        }
        if (value != null && !value.isBlank()) {
          String[] parts = value.split("\\|", 3);
          if (parts.length >= 2) {
            Long userId = parsePositiveLong(parts[0]);
            String role = parts[1];
            if (userId != null) {
              UserContext.set(new UserContext.UserPrincipal(userId, role));
              filterChain.doFilter(request, response);
              return;
            }
          }
        }
      }

      writeUnauthorized(request, response);
    } finally {
      UserContext.clear();
    }
  }

  private boolean isPublicPath(String uri) {
    if (uri == null) return false;
    return uri.startsWith("/api/auth/login")
        || uri.startsWith("/api/auth/register")
        || uri.startsWith("/api/auth/logout")
        || uri.startsWith("/ws");
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

  private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String origin = request.getHeader("Origin");
    if (origin != null && !origin.isBlank()) {
      response.setHeader("Access-Control-Allow-Origin", origin);
      response.setHeader("Vary", "Origin");
      response.setHeader("Access-Control-Allow-Credentials", "true");
    }

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write("{\"success\":false,\"message\":\"Unauthorized\",\"data\":null}");
  }
}

