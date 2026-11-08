-- 用户余额字段（展示型）：个人中心展示账户余额，实际支付仍走现有支付链路。
-- 幂等：字段已存在时跳过。

USE canteen_feedback;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'balance');
SET @s := IF(@c = 0,
    'ALTER TABLE sys_user ADD COLUMN balance DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT ''账户余额（展示型）'' AFTER avatar',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 演示：给现有学生账号填充一个可展示的余额（按账号序号生成，便于演示时看出差异）
UPDATE sys_user
SET balance = 200.00 + (id % 30) * 10.00
WHERE role = 'STUDENT' AND balance = 0.00;

SELECT '用户余额字段已就绪' AS result;
