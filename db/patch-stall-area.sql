-- 档口区域/楼层字段（可重复执行）
-- area: south=南区 north=北区；floor: 1/2（北区固定 1）
USE canteen_feedback;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'canteen_feedback' AND TABLE_NAME = 'canteen_stall' AND COLUMN_NAME = 'area'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE canteen_stall ADD COLUMN area VARCHAR(10) NOT NULL DEFAULT ''south'' COMMENT ''食堂区域：south-南区 north-北区'' AFTER window_no',
    'SELECT ''area 已存在'' AS skip');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'canteen_feedback' AND TABLE_NAME = 'canteen_stall' AND COLUMN_NAME = 'floor'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE canteen_stall ADD COLUMN floor TINYINT NOT NULL DEFAULT 1 COMMENT ''楼层：1/2'' AFTER area',
    'SELECT ''floor 已存在'' AS skip');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @key_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'canteen_feedback' AND TABLE_NAME = 'canteen_stall' AND INDEX_NAME = 'idx_area_floor'
);
SET @sql := IF(@key_exists = 0,
    'ALTER TABLE canteen_stall ADD KEY idx_area_floor (area, floor)',
    'SELECT ''idx_area_floor 已存在'' AS skip');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 现有 12 个档口重新落位：南区 6 个（1楼3 + 2楼3），北区 6 个（1楼）
-- location 里的楼层前缀去掉，只留方位，楼层交给 floor 字段表达
UPDATE canteen_stall SET area = 'south', floor = 1, location = REPLACE(location, '一楼', '') WHERE window_no IN ('01','02','03');
UPDATE canteen_stall SET area = 'south', floor = 2, location = REPLACE(location, '二楼', '') WHERE window_no IN ('04','05','06');
UPDATE canteen_stall SET area = 'north', floor = 1,
    location = REPLACE(REPLACE(REPLACE(location, '一楼', ''), '二楼', ''), '三楼', '')
    WHERE window_no IN ('07','08','09','10','11','12');

SELECT '档口区域楼层迁移完成' AS result;
