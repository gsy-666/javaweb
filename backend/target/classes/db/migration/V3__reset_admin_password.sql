-- Reset admin password to 123456 (BCrypt hash) and ensure ADMIN role binding exists

UPDATE sys_user
SET password_hash = '$2a$10$v1Jf1E8UAA0iSMCno4p8M.ZIp4T2Y5dqgDkY69lJ/5FfKukLJHq8K',
    enabled = 1
WHERE username = 'admin';

INSERT INTO sys_user_role(user_id, role_id)
SELECT u.id, r.id
FROM sys_user u, sys_role r
WHERE u.username='admin' AND r.code='ADMIN'
ON DUPLICATE KEY UPDATE role_id=role_id;
