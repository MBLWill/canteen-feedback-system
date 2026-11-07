-- 个人中心：用户邮箱 + 档口证照（可重复执行）
USE canteen_feedback;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'canteen_feedback' AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'email');
SET @s := IF(@c = 0,
    'ALTER TABLE sys_user ADD COLUMN email VARCHAR(100) NULL COMMENT ''绑定邮箱'' AFTER phone',
    'SELECT ''email 已存在'' AS skip');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'canteen_feedback' AND TABLE_NAME = 'canteen_stall' AND COLUMN_NAME = 'health_cert_url');
SET @s := IF(@c = 0,
    'ALTER TABLE canteen_stall ADD COLUMN health_cert_url VARCHAR(255) NULL COMMENT ''健康证图片'' AFTER hygiene_score',
    'SELECT ''health_cert_url 已存在'' AS skip');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'canteen_feedback' AND TABLE_NAME = 'canteen_stall' AND COLUMN_NAME = 'license_url');
SET @s := IF(@c = 0,
    'ALTER TABLE canteen_stall ADD COLUMN license_url VARCHAR(255) NULL COMMENT ''营业执照图片'' AFTER health_cert_url',
    'SELECT ''license_url 已存在'' AS skip');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SELECT '个人中心字段就绪' AS result;
