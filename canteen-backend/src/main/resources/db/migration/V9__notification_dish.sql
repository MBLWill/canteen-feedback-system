-- ============================================================
-- V9 消息通知补充关联菜品ID（点击通知可跳转到对应菜品详情）
-- ============================================================
ALTER TABLE notification
    ADD COLUMN dish_id BIGINT NULL COMMENT '关联菜品ID' AFTER review_id;
