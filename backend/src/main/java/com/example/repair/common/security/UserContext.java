package com.example.repair.common.security;

import lombok.Value;

public class UserContext {
  private static final ThreadLocal<UserPrincipal> TL = new ThreadLocal<>();

  public static void set(UserPrincipal p) {
    TL.set(p);
  }

  public static UserPrincipal getOptional() {
    return TL.get();
  }

  public static UserPrincipal getRequired() {
    UserPrincipal p = TL.get();
    if (p == null) {
      throw new IllegalStateException("Missing user context. Provide Authorization Bearer token.");
    }
    return p;
  }

  public static void clear() {
    TL.remove();
  }

  @Value
  public static class UserPrincipal {
    Long userId;
    String role;
  }
}

