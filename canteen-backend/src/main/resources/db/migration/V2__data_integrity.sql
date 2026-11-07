-- 数据完整性约束：给存量库补齐唯一索引与评分 CHECK（幂等，可重复执行）
-- 注意：本脚本由迁移器执行，只支持 ; 分隔，不能使用 DELIMITER 存储过程
USE canteen_feedback;

-- sys_user.student_no 唯一
SET @c := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'uk_student_no');
SET @s := IF(@c = 0, 'ALTER TABLE sys_user ADD UNIQUE KEY uk_student_no (student_no)', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- sys_user.email 唯一
SET @c := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'uk_email');
SET @s := IF(@c = 0, 'ALTER TABLE sys_user ADD UNIQUE KEY uk_email (email)', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- sys_user.phone 唯一
SET @c := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'uk_phone');
SET @s := IF(@c = 0, 'ALTER TABLE sys_user ADD UNIQUE KEY uk_phone (phone)', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- canteen_stall.name 唯一
SET @c := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'canteen_stall' AND INDEX_NAME = 'uk_name');
SET @s := IF(@c = 0, 'ALTER TABLE canteen_stall ADD UNIQUE KEY uk_name (name)', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- sys_faq.question 唯一
SET @c := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_faq' AND INDEX_NAME = 'uk_question');
SET @s := IF(@c = 0, 'ALTER TABLE sys_faq ADD UNIQUE KEY uk_question (question)', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- dish_review.rating CHECK 1-5
SET @c := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'dish_review'
      AND CONSTRAINT_NAME = 'chk_rating' AND CONSTRAINT_TYPE = 'CHECK');
SET @s := IF(@c = 0, 'ALTER TABLE dish_review ADD CONSTRAINT chk_rating CHECK (rating >= 1 AND rating <= 5)', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SELECT '数据完整性约束已就绪' AS result;
