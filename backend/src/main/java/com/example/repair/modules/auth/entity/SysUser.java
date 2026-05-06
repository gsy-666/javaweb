package com.example.repair.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_user")
public class SysUser {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String username;

  private String passwordHash;

  private String displayName;

  private String phone;

  private Integer enabled;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}
