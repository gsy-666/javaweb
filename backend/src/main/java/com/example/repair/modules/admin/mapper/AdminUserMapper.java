package com.example.repair.modules.admin.mapper;

import com.example.repair.modules.admin.dto.AdminUserListItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminUserMapper {

  @Select(
      """
      SELECT
        u.id AS user_id,
        r.code AS role,
        COALESCE(NULLIF(u.username, ''), u.phone) AS account,
        u.display_name AS display_name,
        u.phone AS phone,
        u.enabled AS enabled,
        wp.trade_code AS trade_code,
        wp.accept_orders AS accept_orders,
        u.created_at AS created_at
      FROM sys_user u
      JOIN sys_user_role ur ON ur.user_id = u.id
      JOIN sys_role r ON r.id = ur.role_id
      LEFT JOIN worker_profile wp ON wp.user_id = u.id
      WHERE (#{role} IS NULL OR #{role} = '' OR r.code = #{role})
      ORDER BY u.id DESC
      """)
  List<AdminUserListItem> listUsers(@Param("role") String role);
}
