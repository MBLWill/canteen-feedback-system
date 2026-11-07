-- 首页轮播广告：建表 + 初始数据（可直接重复执行）
USE canteen_feedback;

CREATE TABLE IF NOT EXISTS sys_banner (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    stall_id    BIGINT       NULL COMMENT '关联档口ID，为空表示全站公告',
    title       VARCHAR(100) NOT NULL COMMENT '广告标题',
    subtitle    VARCHAR(200) NULL COMMENT '广告副标题',
    image       VARCHAR(500) NULL COMMENT '图片地址，为空则使用背景色渐变',
    bg_color    VARCHAR(30)  NOT NULL DEFAULT '#ff7a1a' COMMENT '背景渐变起始色',
    emoji       VARCHAR(10)  NULL COMMENT '无图时展示的图标',
    link        VARCHAR(200) NULL COMMENT '点击跳转路径，如 /dishes?stallId=1',
    sort_no     INT          NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-展示 0-下架',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_status (status),
    KEY idx_stall (stall_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '首页轮播广告表';

DELETE FROM sys_banner WHERE id <= 100;

INSERT INTO sys_banner (id, stall_id, title, subtitle, image, bg_color, emoji, link, sort_no, status) VALUES
(1, 4, '麻辣香锅 · 今日特惠', '两人同行第二份半价，30+ 种食材自由搭配', '/dishes/17.jpg', '#ff5722', '🍲', '/dishes?stallId=4', 1, 1),
(2, 3, '面食工坊 · 每日现熬汤底', '一清二白三红四绿，清晨 6 点开始吊汤', '/dishes/11.jpg', '#1e88e5', '🍜', '/dishes?stallId=3', 2, 1),
(3, 10, '东北饺子 · 现包现煮', '十余种馅可选，一份就管饱', '/dishes/41.jpg', '#d84315', '🥟', '/dishes?stallId=10', 3, 1),
(4, 2, '粤式烧腊 · 11:00 新鲜出炉', '蜜汁叉烧 / 脆皮烧鸭，每日限量 200 份', '/dishes/8.jpg', '#c62828', '🍖', '/dishes?stallId=2', 4, 1),
(5, 9, '煲仔饭 · 砂锅现煲', '底部那层锅巴，刮起来特别过瘾', '/dishes/37.jpg', '#6d4c41', '🍚', '/dishes?stallId=9', 5, 1),
(6, 11, '自选快餐 · 二十余种热菜', '米饭免费加，赶课也能吃得饱', '/dishes/45.jpg', '#0277bd', '🍱', '/dishes?stallId=11', 6, 1),
(7, 8, '饮品甜品 · 杨枝甘露回归', '当日鲜果现制，可选无糖少冰', '/dishes/33.jpg', '#8e24aa', '🍧', '/dishes?stallId=8', 7, 1),
(8, NULL, '欢迎使用食堂菜品反馈评价系统', '打分、评价、留言，你的每一条反馈我们都会认真对待', '/dishes/banner-welcome.jpg', '#ff7a1a', '🍽️', '/feedbacks', 8, 1);
