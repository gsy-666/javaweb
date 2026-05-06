-- Seed default admin account (admin / 123456)
-- Password hash is BCrypt for "123456"

INSERT INTO sys_user(username, password_hash, display_name, phone, enabled)
VALUES ('admin', '$2a$10$v1Jf1E8UAA0iSMCno4p8M.ZIp4T2Y5dqgDkY69lJ/5FfKukLJHq8K', '管理员', NULL, 1)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), enabled=VALUES(enabled);

INSERT INTO sys_user_role(user_id, role_id)
SELECT u.id, r.id
FROM sys_user u, sys_role r
WHERE u.username='admin' AND r.code='ADMIN'
ON DUPLICATE KEY UPDATE role_id=role_id;
