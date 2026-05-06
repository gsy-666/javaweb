-- Ensure admin exists and reset password to 123456 (BCrypt)

INSERT INTO sys_user(username, password_hash, display_name, phone, enabled)
VALUES ('admin', '$2b$10$Bh07NecFnmKQm.Pl9GReZe0lfY2GFv2nC6/H7Zb/IiB6UVw/BkeDS', '管理员', NULL, 1)
ON DUPLICATE KEY UPDATE password_hash=VALUES(password_hash), display_name=VALUES(display_name), enabled=VALUES(enabled);

INSERT INTO sys_user_role(user_id, role_id)
SELECT u.id, r.id
FROM sys_user u, sys_role r
WHERE u.username='admin' AND r.code='ADMIN'
ON DUPLICATE KEY UPDATE role_id=role_id;
