-- 商家端（档口商家）：账号关联、待办、留言（可重复执行）
USE canteen_feedback;

-- 1. sys_user 加 stall_id（商家关联档口）
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'canteen_feedback' AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'stall_id');
SET @s := IF(@c = 0,
    'ALTER TABLE sys_user ADD COLUMN stall_id BIGINT NULL COMMENT ''商家所属档口ID，仅 MERCHANT 角色使用'' AFTER role',
    'SELECT ''stall_id 已存在'' AS skip');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 2. 商家待办：INSPECT=管理员敦促整改，BAD_REVIEW=差评预警（自动生成）
CREATE TABLE IF NOT EXISTS merchant_todo (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    stall_id    BIGINT       NOT NULL COMMENT '所属档口',
    type        VARCHAR(20)  NOT NULL COMMENT 'INSPECT-整改敦促 BAD_REVIEW-差评预警',
    title       VARCHAR(100) NOT NULL COMMENT '标题',
    content     VARCHAR(500) NULL COMMENT '说明',
    dish_id     BIGINT       NULL COMMENT '差评预警关联的菜品ID',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0-待处理 1-已处理',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    handle_time DATETIME     NULL COMMENT '处理时间',
    KEY idx_stall (stall_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商家待办事项';

-- 3. 商家留言（商家 -> 管理员，管理员可回复）
CREATE TABLE IF NOT EXISTS merchant_message (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT        NOT NULL COMMENT '商家用户ID',
    stall_id    BIGINT        NOT NULL COMMENT '所属档口',
    content     VARCHAR(1000) NOT NULL COMMENT '留言内容',
    status      TINYINT       NOT NULL DEFAULT 0 COMMENT '0-未读 1-已读 2-已回复',
    reply       VARCHAR(1000) NULL COMMENT '管理员回复',
    reply_time  DATETIME      NULL COMMENT '回复时间',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_stall (stall_id),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商家给管理员的留言';

SELECT '商家端表结构就绪' AS result;
