package com.example.repair.modules.admin.service;

import com.example.repair.modules.admin.dto.AdminUserListItem;
import com.example.repair.modules.admin.mapper.AdminUserMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserService {
  private final AdminUserMapper adminUserMapper;

  public List<AdminUserListItem> listUsers(String role) {
    return adminUserMapper.listUsers(role);
  }
}
