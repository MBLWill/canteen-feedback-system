-- 给现有档口批量生成商家账号（可重复执行）
-- 账号 100012~100023（跳过被学生占用的 100011），密码统一 123456（BCrypt）
USE canteen_feedback;

INSERT INTO sys_user (username, password, real_name, role, stall_id, status)
SELECT CONCAT(100011 + s.id), '$2a$10$9a4ogJdOUtpWZADoX3U7c.LtVttldK8A7QsqIxc6dir9UPTmhKIc.',
       CONCAT(s.name, ' 商家'), 'MERCHANT', s.id, 1
FROM canteen_stall s
WHERE s.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_user u
      WHERE u.username = CONCAT(100011 + s.id) AND u.deleted = 0
  );

SELECT u.username, u.real_name, s.name AS stall_name
FROM sys_user u JOIN canteen_stall s ON s.id = u.stall_id
WHERE u.role = 'MERCHANT' AND u.deleted = 0
ORDER BY u.username;
