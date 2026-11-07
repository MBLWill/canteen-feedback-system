-- 购物车与支付：建表 + dining_order 扩展（可重复执行）
USE canteen_feedback;

CREATE TABLE IF NOT EXISTS cart_item (
    id          BIGINT   PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT   NOT NULL COMMENT '所属用户',
    dish_id     BIGINT   NOT NULL COMMENT '菜品ID',
    stall_id    BIGINT   NOT NULL COMMENT '所属档口ID，冗余一份便于按档口分组结算',
    quantity    INT      NOT NULL DEFAULT 1 COMMENT '份数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_dish (user_id, dish_id),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '购物车条目表';

CREATE TABLE IF NOT EXISTS pay_record (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    pay_no      VARCHAR(64)   NOT NULL COMMENT '商户订单号，传给支付平台',
    order_id    BIGINT        NOT NULL COMMENT '关联 dining_order.id',
    user_id     BIGINT        NOT NULL COMMENT '付款用户',
    channel     VARCHAR(20)   NOT NULL COMMENT '支付通道：mock / alipay / wechat',
    amount      DECIMAL(10, 2) NOT NULL COMMENT '支付金额',
    status      VARCHAR(20)   NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED-待支付 PAID-已支付 CLOSED-已关闭',
    trade_no    VARCHAR(64)   NULL COMMENT '支付平台交易号',
    pay_url     VARCHAR(1000) NULL COMMENT '支付跳转地址或二维码内容',
    raw_notify  TEXT          NULL COMMENT '回调原始报文，排障用',
    paid_time   DATETIME      NULL COMMENT '支付成功时间',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_pay_no (pay_no),
    KEY idx_order (order_id),
    KEY idx_user (user_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '支付流水表';

DROP PROCEDURE IF EXISTS add_col_if_missing;

DELIMITER $$
CREATE PROCEDURE add_col_if_missing(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl VARCHAR(500))
BEGIN
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE()
                    AND TABLE_NAME = tbl
                    AND COLUMN_NAME = col) THEN
        SET @ddl = CONCAT('ALTER TABLE ', tbl, ' ADD COLUMN ', ddl);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_col_if_missing('dining_order', 'pay_status',
                        'pay_status VARCHAR(20) NOT NULL DEFAULT ''UNPAID'' COMMENT ''UNPAID-待支付 PAID-已支付 CLOSED-已关闭''');
CALL add_col_if_missing('dining_order', 'pay_no', 'pay_no VARCHAR(64) NULL COMMENT ''最近一次支付单号''');
CALL add_col_if_missing('dining_order', 'pay_channel', 'pay_channel VARCHAR(20) NULL COMMENT ''支付通道''');
CALL add_col_if_missing('dining_order', 'paid_time', 'paid_time DATETIME NULL COMMENT ''支付成功时间''');
CALL add_col_if_missing('dining_order', 'expire_time', 'expire_time DATETIME NULL COMMENT ''支付截止时间''');

DROP PROCEDURE IF EXISTS add_col_if_missing;

SHOW COLUMNS FROM dining_order;
