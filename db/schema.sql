-- 校园食堂菜品反馈评价系统 - 数据库结构
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS canteen_feedback
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE canteen_feedback;

-- 用户表（学生 + 食堂管理员）
CREATE TABLE IF NOT EXISTS sys_user (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username     VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password     VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
    real_name    VARCHAR(50)  NOT NULL COMMENT '姓名',
    role         VARCHAR(20)  NOT NULL DEFAULT 'STUDENT' COMMENT '角色：STUDENT-学生 ADMIN-食堂管理员 MERCHANT-商家',
    stall_id     BIGINT       NULL COMMENT '商家所属档口ID，仅 MERCHANT 角色使用',
    student_no   VARCHAR(30)  NULL COMMENT '学号',
    phone        VARCHAR(20)  NULL COMMENT '手机号',
    email        VARCHAR(100) NULL COMMENT '绑定邮箱',
    avatar       VARCHAR(255) NULL COMMENT '头像URL',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_student_no (student_no),
    UNIQUE KEY uk_email (email),
    UNIQUE KEY uk_phone (phone),
    KEY idx_role (role),
    KEY idx_stall (stall_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- 商家待办事项
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

-- 商家留言（商家 -> 管理员）
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

-- 菜品每日销售记录
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

-- 食堂档口/窗口表
CREATE TABLE IF NOT EXISTS canteen_stall (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    name         VARCHAR(100) NOT NULL COMMENT '档口名称',
    window_no    VARCHAR(20)  NOT NULL COMMENT '窗口编号',
    area         VARCHAR(10)  NOT NULL DEFAULT 'south' COMMENT '食堂区域：south-南区 north-北区',
    floor        TINYINT      NOT NULL DEFAULT 1 COMMENT '楼层：1/2，北区固定 1',
    location     VARCHAR(100) NULL COMMENT '楼层内方位（东侧/西侧/中部等）',
    category     VARCHAR(50)  NULL COMMENT '主营品类',
    manager_name VARCHAR(50)  NULL COMMENT '档口负责人姓名',
    manager_phone VARCHAR(20) NULL COMMENT '档口负责人联系电话',
    cover_image  VARCHAR(255) NULL COMMENT '封面图URL',
    description  VARCHAR(500) NULL COMMENT '档口简介',
    hygiene_score DECIMAL(4, 1) NOT NULL DEFAULT 85.0 COMMENT '学校评定的卫生分（0-100），低于80需整改',
    health_cert_url VARCHAR(255) NULL COMMENT '健康证图片',
    license_url  VARCHAR(255) NULL COMMENT '营业执照图片',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-营业 0-歇业',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_window_no (window_no),
    UNIQUE KEY uk_name (name),
    KEY idx_area_floor (area, floor)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '食堂档口表';

-- 菜品表
CREATE TABLE IF NOT EXISTS dish (
    id           BIGINT         PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    stall_id     BIGINT         NOT NULL COMMENT '所属档口ID',
    name         VARCHAR(100)   NOT NULL COMMENT '菜品名称',
    price        DECIMAL(8, 2)  NOT NULL DEFAULT 0 COMMENT '价格',
    category     VARCHAR(50)    NULL COMMENT '分类：主食/荤菜/素菜/汤品/小吃/饮品',
    image        VARCHAR(255)   NULL COMMENT '菜品图片URL',
    description  VARCHAR(500)   NULL COMMENT '菜品描述',
    featured     TINYINT        NOT NULL DEFAULT 0 COMMENT '招牌菜：1-是 0-否',
    status       TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：1-在售 0-下架',
    create_time  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    active_name  VARCHAR(100)   GENERATED ALWAYS AS (IF(deleted = 0, name, NULL)) STORED COMMENT '未删除行的菜名，用于同档口唯一约束',
    KEY idx_stall (stall_id),
    UNIQUE KEY uk_stall_active_name (stall_id, active_name),
    KEY idx_category (category),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '菜品表';

-- 菜品评价表
CREATE TABLE IF NOT EXISTS dish_review (
    id           BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    dish_id      BIGINT        NOT NULL COMMENT '菜品ID',
    user_id      BIGINT        NOT NULL COMMENT '评价人ID',
    rating       TINYINT       NOT NULL COMMENT '评分：1-5星',
    content      VARCHAR(1000) NULL COMMENT '评价内容',
    anonymous    TINYINT       NOT NULL DEFAULT 0 COMMENT '是否匿名：1-是 0-否',
    status       TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：0-待审核 1-已发布 2-已屏蔽',
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_dish_user (dish_id, user_id),
    KEY idx_dish (dish_id),
    KEY idx_user (user_id),
    KEY idx_status (status),
    CONSTRAINT chk_rating CHECK (rating >= 1 AND rating <= 5)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '菜品评价表';

-- 留言反馈表
CREATE TABLE IF NOT EXISTS feedback (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT       NOT NULL COMMENT '留言人ID',
    stall_id      BIGINT       NULL COMMENT '关联档口ID',
    title         VARCHAR(200) NOT NULL COMMENT '标题',
    content       VARCHAR(2000) NOT NULL COMMENT '内容',
    type          VARCHAR(20)  NOT NULL DEFAULT 'SUGGEST' COMMENT '类型：SUGGEST-建议 COMPLAINT-投诉 PRAISE-表扬 OTHER-其他',
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待处理 PROCESSING-处理中 RESOLVED-已解决',
    reply_content VARCHAR(1000) NULL COMMENT '管理员回复',
    reply_user_id BIGINT       NULL COMMENT '回复人ID',
    reply_time    DATETIME     NULL COMMENT '回复时间',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_user (user_id),
    KEY idx_stall (stall_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '留言反馈表';

-- 公告表
CREATE TABLE IF NOT EXISTS sys_notice (
    id            BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    title         VARCHAR(200) NOT NULL COMMENT '公告标题',
    content       TEXT        NULL COMMENT '公告内容',
    publisher_id  BIGINT      NULL COMMENT '发布人ID',
    status        TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1-发布 0-草稿',
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '公告表';

-- 菜品收藏表
CREATE TABLE IF NOT EXISTS dish_favorite (
    id          BIGINT   PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    dish_id     BIGINT   NOT NULL COMMENT '菜品ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    UNIQUE KEY uk_user_dish (user_id, dish_id),
    KEY idx_dish (dish_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '菜品收藏表';

-- 登录日志表
CREATE TABLE IF NOT EXISTS login_log (
    id          BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50) NULL COMMENT '登录账号',
    real_name   VARCHAR(50) NULL COMMENT '姓名',
    role        VARCHAR(20) NULL COMMENT '角色',
    ip          VARCHAR(50) NULL COMMENT '登录IP',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '结果：1-成功 0-失败',
    login_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    KEY idx_time (login_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '登录日志表';

-- 首页轮播广告表
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

-- 订单主表
CREATE TABLE IF NOT EXISTS dining_order (
    id           BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_no     VARCHAR(32)   NOT NULL COMMENT '订单号',
    user_id      BIGINT        NOT NULL COMMENT '下单学生ID',
    stall_id     BIGINT        NOT NULL COMMENT '所属档口ID',
    pickup_time  DATETIME      NOT NULL COMMENT '预约取餐时间',
    remark       VARCHAR(200)  NULL COMMENT '备注：口味、忌口等',
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '订单总金额',
    status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待取餐 DONE-已完成 CANCELED-已取消',
    pay_status   VARCHAR(20)   NOT NULL DEFAULT 'UNPAID' COMMENT '支付状态：UNPAID-待支付 PAID-已支付 CLOSED-已关闭',
    pay_no       VARCHAR(64)   NULL COMMENT '最近一次支付单号',
    pay_channel  VARCHAR(20)   NULL COMMENT '支付通道',
    paid_time    DATETIME      NULL COMMENT '支付成功时间',
    expire_time  DATETIME      NULL COMMENT '支付截止时间',
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user (user_id),
    KEY idx_stall (stall_id),
    KEY idx_status (status),
    KEY idx_pickup (pickup_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单主表';

-- 订单明细表
CREATE TABLE IF NOT EXISTS dining_order_item (
    id         BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_id   BIGINT        NOT NULL COMMENT '订单ID',
    dish_id    BIGINT        NOT NULL COMMENT '菜品ID',
    dish_name  VARCHAR(100)  NOT NULL COMMENT '下单时的菜品名称快照',
    price      DECIMAL(8, 2) NOT NULL COMMENT '下单时的单价快照',
    quantity   INT           NOT NULL DEFAULT 1 COMMENT '份数',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted    TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_order (order_id),
    KEY idx_dish (dish_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单明细表';

-- 智能客服知识库（本地问答，关键词匹配，不依赖外部大模型）
CREATE TABLE IF NOT EXISTS sys_faq (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    category   VARCHAR(30)  NOT NULL DEFAULT 'OTHER' COMMENT '分类：EVALUATE-评价 FEEDBACK-投诉 RULE-规则 ORDER-预约 OTHER-其他',
    question   VARCHAR(200) NOT NULL COMMENT '标准问法',
    answer     VARCHAR(2000) NOT NULL COMMENT '答案正文',
    keywords   VARCHAR(500) NULL COMMENT '匹配关键词，逗号分隔',
    sort_no    INT          NOT NULL DEFAULT 0 COMMENT '排序号，降序',
    status     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-停用',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_question (question),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '智能客服知识库问答表';

-- 智能客服问答日志（用于命中率统计与未命中问题补录）
CREATE TABLE IF NOT EXISTS chat_log (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id    BIGINT       NULL COMMENT '提问用户',
    question   VARCHAR(500) NOT NULL COMMENT '原始提问',
    intent     VARCHAR(50)  NULL COMMENT '命中意图编码',
    intent_label VARCHAR(50) NULL COMMENT '命中意图名称',
    hit        TINYINT      NOT NULL DEFAULT 0 COMMENT '是否命中：1-命中 0-未命中',
    cost_ms    INT          NOT NULL DEFAULT 0 COMMENT '响应耗时（毫秒）',
    helpful    TINYINT      NULL COMMENT '用户评价：1-有帮助 0-没帮助',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_user (user_id),
    KEY idx_hit (hit),
    KEY idx_create (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '智能客服问答日志表';

-- 智能客服待办（未命中/差评问题补录，管理员处理）
CREATE TABLE IF NOT EXISTS chat_todo (
    id           BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    log_id       BIGINT      NULL COMMENT '来源问答记录ID',
    user_id      BIGINT      NULL COMMENT '提问学生ID',
    question     VARCHAR(500) NOT NULL COMMENT '学生的原始提问',
    kind         VARCHAR(20) NOT NULL DEFAULT 'REAL' COMMENT '提问分类：NOISE/SOCIAL/REAL',
    reason       VARCHAR(20) NOT NULL DEFAULT 'UNMATCHED' COMMENT '进待办原因：UNMATCHED-没听懂 UNHELPFUL-用户点没帮助 URGENT-紧急投诉',
    intent_label VARCHAR(50) NULL COMMENT '当时识别出的意图',
    priority     TINYINT     NOT NULL DEFAULT 1 COMMENT '优先级：1-普通 2-紧急',
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待处理 HANDLING-处理中 DONE-已解决 IGNORED-已忽略',
    hits         INT         NOT NULL DEFAULT 1 COMMENT '相同问题被问到的次数（去重合并）',
    note         VARCHAR(500) NULL COMMENT '管理员处理备注 / 给学生的回复',
    faq_id       BIGINT      NULL COMMENT '补录进知识库后的问答ID',
    handler_id   BIGINT      NULL COMMENT '处理人ID',
    handle_time  DATETIME    NULL COMMENT '处理时间',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_log (log_id),
    KEY idx_status (status),
    KEY idx_priority (priority),
    KEY idx_create (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '智能客服待办表';

-- 购物车条目表
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

-- 支付流水表
CREATE TABLE IF NOT EXISTS pay_record (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    pay_no      VARCHAR(64)   NOT NULL COMMENT '商户订单号，传给支付平台',
    order_id    BIGINT        NOT NULL COMMENT '关联 dining_order.id',
    user_id     BIGINT        NOT NULL COMMENT '付款用户',
    channel     VARCHAR(20)   NOT NULL COMMENT '支付通道：mock / alipay / wechat',
    amount      DECIMAL(10, 2) NOT NULL COMMENT '支付金额',
    status      VARCHAR(20)   NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED-待支付 PAID-已支付 CLOSED-已关闭',
    trade_no    VARCHAR(64)   NULL COMMENT '支付平台交易号',
    pay_url     VARCHAR(4000) NULL COMMENT '支付跳转地址或二维码内容（支付宝沙箱收银台 URL 含签名，需足够长）',
    raw_notify  TEXT          NULL COMMENT '回调原始报文，排障用',
    paid_time   DATETIME      NULL COMMENT '支付成功时间',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_pay_no (pay_no),
    KEY idx_order (order_id),
    KEY idx_user (user_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '支付流水表';
