package com.example.repair.modules.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.repair.common.exception.BizException;
import com.example.repair.modules.auth.dto.AuthLoginRequest;
import com.example.repair.modules.auth.dto.AuthLoginResponse;
import com.example.repair.modules.auth.dto.AuthRegisterRequest;
import com.example.repair.modules.auth.entity.SysRole;
import com.example.repair.modules.auth.entity.SysUser;
import com.example.repair.modules.auth.entity.SysUserRole;
import com.example.repair.modules.auth.mapper.SysRoleMapper;
import com.example.repair.modules.auth.mapper.SysUserMapper;
import com.example.repair.modules.auth.mapper.SysUserRoleMapper;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private static final Duration TOKEN_TTL = Duration.ofDays(7);

  private final SysUserMapper sysUserMapper;
  private final SysRoleMapper sysRoleMapper;
  private final SysUserRoleMapper sysUserRoleMapper;
  private final StringRedisTemplate redis;

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private final SecureRandom secureRandom = new SecureRandom();

  @Transactional
  public AuthLoginResponse register(AuthRegisterRequest req) {
    String account = safeTrim(req.getAccount());
    if (account.isEmpty()) throw new BizException("请输入账号");

    String role = safeTrim(req.getRole()).toUpperCase();
    if (!(Objects.equals(role, "USER") || Objects.equals(role, "WORKER"))) {
      throw new BizException("不允许注册该身份");
    }

    if (existsUsername(account) || existsPhone(account)) {
      throw new BizException("账号已存在");
    }

    SysUser u = new SysUser();
    u.setUsername(account);
    u.setPhone(isPhone(account) ? account : null);
    u.setDisplayName(safeTrim(req.getDisplayName()).isEmpty() ? account : safeTrim(req.getDisplayName()));
    u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
    u.setEnabled(1);

    sysUserMapper.insert(u);

    SysRole r = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, role));
    if (r == null) throw new BizException("角色不存在");

    SysUserRole ur = new SysUserRole();
    ur.setUserId(u.getId());
    ur.setRoleId(r.getId());
    sysUserRoleMapper.insert(ur);

    String token = issueToken(u.getId(), role, u.getDisplayName());
    return new AuthLoginResponse(u.getId(), role, u.getDisplayName(), token);
  }

  public AuthLoginResponse login(AuthLoginRequest req) {
    String account = safeTrim(req.getAccount());
    SysUser u = sysUserMapper.selectOne(
        new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUsername, account)
            .or()
            .eq(SysUser::getPhone, account));

    if (u == null) throw new BizException("账号或密码错误");
    if (u.getEnabled() != null && u.getEnabled() == 0) throw new BizException("账号已被禁用");
    if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
      throw new BizException("账号或密码错误");
    }

    SysUserRole ur = sysUserRoleMapper.selectOne(
        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, u.getId()));
    if (ur == null) throw new BizException("账号未分配角色");

    SysRole r = sysRoleMapper.selectById(ur.getRoleId());
    if (r == null) throw new BizException("账号角色不存在");

    String role = r.getCode();
    String token = issueToken(u.getId(), role, u.getDisplayName());
    return new AuthLoginResponse(u.getId(), role, u.getDisplayName(), token);
  }

  public void logout(String token) {
    if (token == null || token.isBlank()) return;
    redis.delete(tokenKey(token));
  }

  private String issueToken(Long userId, String role, String displayName) {
    String token = generateToken();
    String value = userId + "|" + role + "|" + (displayName == null ? "" : displayName);
    redis.opsForValue().set(tokenKey(token), value, TOKEN_TTL);
    return token;
  }

  private String tokenKey(String token) {
    return "sr:token:" + token;
  }

  private String generateToken() {
    byte[] buf = new byte[32];
    secureRandom.nextBytes(buf);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
  }

  private boolean existsUsername(String username) {
    Long cnt = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    return cnt != null && cnt > 0;
  }

  private boolean existsPhone(String phone) {
    Long cnt = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone));
    return cnt != null && cnt > 0;
  }

  private static String safeTrim(String s) {
    return s == null ? "" : s.trim();
  }

  private static boolean isPhone(String s) {
    return s != null && s.matches("^1\\d{10}$");
  }
}
