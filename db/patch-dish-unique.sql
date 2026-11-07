-- 菜品同档口重名唯一约束（可重复执行）
-- 逻辑删除用 deleted 标记，普通唯一键会把已删除的行也算进去，
-- 这里用 STORED 生成列：未删除时取菜名，已删除时取 NULL，MySQL 唯一索引允许多个 NULL。

USE canteen_feedback;

-- 1. 先把已有的活跃重名数据收敛掉：同档口同名只保留 id 最小的一条
--    被收敛掉的菜品先从购物车移除（历史订单存的是菜名快照，不受影响）
DELETE c
FROM cart_item c
    JOIN (
        SELECT d.id
        FROM dish d
            JOIN (
                SELECT stall_id, name, MIN(id) AS keep_id
                FROM dish
                WHERE deleted = 0
                GROUP BY stall_id, name
                HAVING COUNT(*) > 1
            ) dup ON dup.stall_id = d.stall_id AND dup.name = d.name
        WHERE d.deleted = 0 AND d.id <> dup.keep_id
    ) gone ON gone.id = c.dish_id;

UPDATE dish d
    JOIN (
        SELECT stall_id, name, MIN(id) AS keep_id
        FROM dish
        WHERE deleted = 0
        GROUP BY stall_id, name
        HAVING COUNT(*) > 1
    ) dup ON dup.stall_id = d.stall_id AND dup.name = d.name
SET d.deleted = 1
WHERE d.id <> dup.keep_id;

-- 2. 加生成列（已存在则跳过）
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'canteen_feedback' AND TABLE_NAME = 'dish' AND COLUMN_NAME = 'active_name'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE dish ADD COLUMN active_name VARCHAR(100) GENERATED ALWAYS AS (IF(deleted = 0, name, NULL)) STORED COMMENT ''未删除行的菜名，用于同档口唯一约束''',
    'SELECT ''active_name 已存在'' AS skip');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 加唯一索引（已存在则跳过）
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'canteen_feedback' AND TABLE_NAME = 'dish' AND INDEX_NAME = 'uk_stall_active_name'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE dish ADD UNIQUE KEY uk_stall_active_name (stall_id, active_name)',
    'SELECT ''uk_stall_active_name 已存在'' AS skip');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'dish 唯一约束处理完成' AS result;
