-- 商家端-学生评价：给 dish_review 增加商家回复与处理状态（幂等，可重复执行）
-- 注意：本脚本由迁移器执行，只支持 ; 分隔，不能使用 DELIMITER 存储过程
USE canteen_feedback;

-- 商家回复内容
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dish_review' AND COLUMN_NAME = 'merchant_reply');
SET @s := IF(@c = 0,
    'ALTER TABLE dish_review ADD COLUMN merchant_reply VARCHAR(500) NULL COMMENT ''商家回复内容'' AFTER status',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 商家回复时间
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dish_review' AND COLUMN_NAME = 'merchant_reply_time');
SET @s := IF(@c = 0,
    'ALTER TABLE dish_review ADD COLUMN merchant_reply_time DATETIME NULL COMMENT ''商家回复时间'' AFTER merchant_reply',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 是否已处理：0-待处理 1-已处理
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dish_review' AND COLUMN_NAME = 'handled');
SET @s := IF(@c = 0,
    'ALTER TABLE dish_review ADD COLUMN handled TINYINT NOT NULL DEFAULT 0 COMMENT ''商家处理状态：0-待处理 1-已处理'' AFTER merchant_reply_time',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 商家端按档口+处理状态分页查询的辅助索引
SET @c := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dish_review' AND INDEX_NAME = 'idx_handled');
SET @s := IF(@c = 0, 'ALTER TABLE dish_review ADD KEY idx_handled (handled)', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SELECT '商家评价回复字段已就绪' AS result;
