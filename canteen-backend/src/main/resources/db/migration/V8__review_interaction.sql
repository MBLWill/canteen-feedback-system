-- ============================================================
-- V8 评价互动：点赞、浏览量、学生间回复、消息通知
-- 1) dish_review 增加 like_count / view_count / reply_count
-- 2) 新建 review_like   评价点赞记录（防重复点赞）
-- 3) 新建 review_reply  评价回复（学生间追问/回答）
-- 4) 新建 notification  消息通知（点赞/回复/商家回复）
-- ============================================================

-- 1. dish_review 增加互动统计字段（IF NOT EXISTS 兼容 MySQL 8 不支持 ADD COLUMN IF NOT EXISTS 的情况，
--    改用信息模式判断，避免重复执行报错；此处用简单 ADD，迁移器保证只执行一次）
ALTER TABLE dish_review
    ADD COLUMN like_count  INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    ADD COLUMN view_count  INT NOT NULL DEFAULT 0 COMMENT '浏览量',
    ADD COLUMN reply_count INT NOT NULL DEFAULT 0 COMMENT '学生回复数';

-- 2. 评价点赞记录表（(review_id, user_id) 唯一，防重复点赞）
CREATE TABLE IF NOT EXISTS review_like (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    review_id   BIGINT       NOT NULL COMMENT '评价ID',
    user_id     BIGINT       NOT NULL COMMENT '点赞用户ID',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_user (review_id, user_id),
    KEY idx_review (review_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '评价点赞记录';

-- 3. 评价回复表（学生对评价的追问/回答，一层回复不嵌套）
CREATE TABLE IF NOT EXISTS review_reply (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    review_id   BIGINT       NOT NULL COMMENT '评价ID',
    user_id     BIGINT       NOT NULL COMMENT '回复用户ID',
    content     VARCHAR(500) NOT NULL COMMENT '回复内容',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_review (review_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '评价回复';

-- 4. 消息通知表
CREATE TABLE IF NOT EXISTS notification (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL COMMENT '接收者用户ID',
    type        VARCHAR(20)  NOT NULL COMMENT 'LIKE=点赞 REPLAY=学生回复 MERCHANT_REPLY=商家回复',
    review_id   BIGINT       NULL COMMENT '关联评价ID',
    reply_id    BIGINT       NULL COMMENT '关联回复ID',
    content     VARCHAR(500) NULL COMMENT '通知内容摘要',
    is_read     TINYINT      NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_read (user_id, is_read)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '消息通知';
