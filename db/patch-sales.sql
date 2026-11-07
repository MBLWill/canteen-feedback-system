-- 商家销售统计表 + 14 天演示数据（可重复执行）
USE canteen_feedback;

CREATE TABLE IF NOT EXISTS dish_sales (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    stall_id    BIGINT        NOT NULL COMMENT '所属档口',
    dish_id     BIGINT        NOT NULL COMMENT '菜品ID',
    dish_name   VARCHAR(100)  NOT NULL COMMENT '菜品名（快照）',
    sale_date   DATE          NOT NULL COMMENT '销售日期',
    quantity    INT           NOT NULL DEFAULT 0 COMMENT '当日销量份数',
    amount      DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '当日销售额',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_stall_date (stall_id, sale_date),
    KEY idx_dish (dish_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '菜品每日销售记录';

-- 给 100012 商家（川味小炒 stall_id=1）生成最近 14 天的演示销量
DELETE FROM dish_sales WHERE stall_id = 1;

INSERT INTO dish_sales (stall_id, dish_id, dish_name, sale_date, quantity, amount)
SELECT s.stall_id, s.dish_id, s.dish_name, s.sale_date, s.qty, ROUND(s.qty * s.price, 2)
FROM (
    SELECT d.stall_id, d.id AS dish_id, d.name AS dish_name, d.price,
           DATE_SUB(CURDATE(), INTERVAL seq.d DAY) AS sale_date,
           FLOOR(RAND() * 18 + 2) AS qty
    FROM dish d
    JOIN (
        SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
        UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13
    ) seq
    WHERE d.stall_id = 1 AND d.deleted = 0 AND d.status = 1
) s;

SELECT stall_id, COUNT(*) AS rows_cnt, MIN(sale_date) AS from_date, MAX(sale_date) AS to_date,
       SUM(quantity) AS total_qty, SUM(amount) AS total_amount
FROM dish_sales
WHERE stall_id = 1
GROUP BY stall_id;
