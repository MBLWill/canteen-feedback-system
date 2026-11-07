-- 订单支付字段自愈：老库用早期 schema.sql 建表时没有 patch-cart-pay.sql 的 5 个列，
-- 导致 StallMapper / DishMapper / UserMapper / OrderMapper 里引用 o.pay_status 的查询
-- 全部抛 "Unknown column 'o.pay_status'"，表现为 /stalls、/dishes 等接口 code=500。
-- 本脚本幂等：列已存在时执行 SELECT 1，可随每次启动的自动迁移重复执行。
-- 注意：由迁移器执行，只支持 ; 分隔，不能用 DELIMITER / 存储过程。

USE canteen_feedback;

CREATE TABLE IF NOT EXISTS cart_item (
    id          BIGINT   PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT   NOT NULL COMMENT '所属用户',
    dish_id     BIGINT   NOT NULL COMMENT '菜品ID',
    stall_id    BIGINT   NOT NULL COMMENT '所属档口ID',
    quantity    INT      NOT NULL DEFAULT 1 COMMENT '份数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_dish (user_id, dish_id),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '购物车条目表';

CREATE TABLE IF NOT EXISTS pay_record (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    pay_no      VARCHAR(64)   NOT NULL COMMENT '商户订单号',
    order_id    BIGINT        NOT NULL COMMENT '关联 dining_order.id',
    user_id     BIGINT        NOT NULL COMMENT '付款用户',
    channel     VARCHAR(20)   NOT NULL COMMENT '支付通道',
    amount      DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    status      VARCHAR(20)   NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/PAID/CLOSED',
    trade_no    VARCHAR(64)   NULL COMMENT '支付平台交易号',
    pay_url     VARCHAR(4000) NULL COMMENT '支付跳转地址或二维码内容',
    raw_notify  TEXT          NULL COMMENT '回调原始报文',
    paid_time   DATETIME      NULL COMMENT '支付成功时间',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_pay_no (pay_no),
    KEY idx_order (order_id),
    KEY idx_user (user_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '支付流水表';

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dining_order' AND COLUMN_NAME = 'pay_status');
SET @s := IF(@c = 0,
    'ALTER TABLE dining_order ADD COLUMN pay_status VARCHAR(20) NOT NULL DEFAULT ''UNPAID'' COMMENT ''UNPAID-待支付 PAID-已支付 CLOSED-已关闭'' AFTER status',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dining_order' AND COLUMN_NAME = 'pay_no');
SET @s := IF(@c = 0, 'ALTER TABLE dining_order ADD COLUMN pay_no VARCHAR(64) NULL COMMENT ''最近一次支付单号''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dining_order' AND COLUMN_NAME = 'pay_channel');
SET @s := IF(@c = 0, 'ALTER TABLE dining_order ADD COLUMN pay_channel VARCHAR(20) NULL COMMENT ''支付通道''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dining_order' AND COLUMN_NAME = 'paid_time');
SET @s := IF(@c = 0, 'ALTER TABLE dining_order ADD COLUMN paid_time DATETIME NULL COMMENT ''支付成功时间''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dining_order' AND COLUMN_NAME = 'expire_time');
SET @s := IF(@c = 0, 'ALTER TABLE dining_order ADD COLUMN expire_time DATETIME NULL COMMENT ''支付截止时间''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SELECT '订单支付字段已就绪' AS result;
