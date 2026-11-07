-- 存量库结构自愈：把历次 patch-*.sql 的结构变更一次性补齐（幂等）
--
-- 背景：老库是用早期版本的 schema.sql 建的，缺了后来加的字段与表；
-- 而 V1__renumber_windows.sql 的末尾 SELECT 直接引用了 s.area / s.floor，
-- 在老库上会抛 "Unknown column 'area'"，导致后端启动被迁移器中断。
-- 本脚本版本号 0，保证在 V1/V2 之前执行，先补齐结构再让后续迁移正常运行。
--
-- 约束：由迁移器执行，只支持 ; 分隔，不能用 DELIMITER / 存储过程；
-- 用 information_schema + PREPARE 做「不存在才加」的幂等判断。

USE canteen_feedback;

-- ---------------------------------------------------------------------------
-- 1. canteen_stall：区域、楼层、证照字段
-- ---------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'canteen_stall' AND COLUMN_NAME = 'area');
SET @s := IF(@c = 0,
    'ALTER TABLE canteen_stall ADD COLUMN area VARCHAR(10) NOT NULL DEFAULT ''south'' COMMENT ''食堂区域：south-南区 north-北区'' AFTER window_no',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'canteen_stall' AND COLUMN_NAME = 'floor');
SET @s := IF(@c = 0,
    'ALTER TABLE canteen_stall ADD COLUMN floor TINYINT NOT NULL DEFAULT 1 COMMENT ''楼层：1/2，北区固定 1'' AFTER area',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'canteen_stall' AND COLUMN_NAME = 'health_cert_url');
SET @s := IF(@c = 0,
    'ALTER TABLE canteen_stall ADD COLUMN health_cert_url VARCHAR(255) NULL COMMENT ''健康证图片'' AFTER hygiene_score',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'canteen_stall' AND COLUMN_NAME = 'license_url');
SET @s := IF(@c = 0,
    'ALTER TABLE canteen_stall ADD COLUMN license_url VARCHAR(255) NULL COMMENT ''营业执照图片'' AFTER health_cert_url',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'canteen_stall' AND INDEX_NAME = 'idx_area_floor');
SET @s := IF(@c = 0,
    'ALTER TABLE canteen_stall ADD KEY idx_area_floor (area, floor)',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ---------------------------------------------------------------------------
-- 2. sys_user：商家关联档口、绑定邮箱
-- ---------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'stall_id');
SET @s := IF(@c = 0,
    'ALTER TABLE sys_user ADD COLUMN stall_id BIGINT NULL COMMENT ''商家所属档口ID，仅 MERCHANT 角色使用'' AFTER role',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'email');
SET @s := IF(@c = 0,
    'ALTER TABLE sys_user ADD COLUMN email VARCHAR(100) NULL COMMENT ''绑定邮箱'' AFTER phone',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ---------------------------------------------------------------------------
-- 3. 缺失的业务表（schema.sql 的 CREATE TABLE IF NOT EXISTS 语义）
-- ---------------------------------------------------------------------------
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

CREATE TABLE IF NOT EXISTS chat_todo (
    id           BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    log_id       BIGINT      NULL COMMENT '来源问答记录ID',
    user_id      BIGINT      NULL COMMENT '提问学生ID',
    question     VARCHAR(500) NOT NULL COMMENT '学生的原始提问',
    kind         VARCHAR(20) NOT NULL DEFAULT 'REAL' COMMENT '提问分类：NOISE/SOCIAL/REAL',
    reason       VARCHAR(20) NOT NULL DEFAULT 'UNMATCHED' COMMENT '进待办原因',
    intent_label VARCHAR(50) NULL COMMENT '当时识别出的意图',
    priority     TINYINT     NOT NULL DEFAULT 1 COMMENT '优先级：1-普通 2-紧急',
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/HANDLING/DONE/IGNORED',
    hits         INT         NOT NULL DEFAULT 1 COMMENT '相同问题被问到的次数',
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

-- ---------------------------------------------------------------------------
-- 4. dish：同档口重名唯一约束（生成列 + 唯一索引）
--    先把已有的活跃重名收敛掉，否则加唯一索引会失败
-- ---------------------------------------------------------------------------
DELETE c
FROM cart_item c
    JOIN (
        SELECT d.id
        FROM dish d
            JOIN (
                SELECT stall_id, name, MIN(id) AS keep_id
                FROM dish WHERE deleted = 0
                GROUP BY stall_id, name HAVING COUNT(*) > 1
            ) dup ON dup.stall_id = d.stall_id AND dup.name = d.name
        WHERE d.deleted = 0 AND d.id <> dup.keep_id
    ) gone ON gone.id = c.dish_id;

UPDATE dish d
    JOIN (
        SELECT stall_id, name, MIN(id) AS keep_id
        FROM dish WHERE deleted = 0
        GROUP BY stall_id, name HAVING COUNT(*) > 1
    ) dup ON dup.stall_id = d.stall_id AND dup.name = d.name
SET d.deleted = 1
WHERE d.id <> dup.keep_id;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dish' AND COLUMN_NAME = 'active_name');
SET @s := IF(@c = 0,
    'ALTER TABLE dish ADD COLUMN active_name VARCHAR(100) GENERATED ALWAYS AS (IF(deleted = 0, name, NULL)) STORED COMMENT ''未删除行的菜名，用于同档口唯一约束''',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dish' AND INDEX_NAME = 'uk_stall_active_name');
SET @s := IF(@c = 0,
    'ALTER TABLE dish ADD UNIQUE KEY uk_stall_active_name (stall_id, active_name)',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ---------------------------------------------------------------------------
-- 5. 数据落位：窗口号 -> 区域/楼层（兼容新旧两种编号，V1 随后会重编号）
--    S1xx / 01-03 = 南区 1 楼；S2xx / 04-06 = 南区 2 楼；N1xx / 07-12 = 北区 1 楼
-- ---------------------------------------------------------------------------
UPDATE canteen_stall
SET area = CASE
        WHEN window_no LIKE 'N%' OR window_no IN ('07','08','09','10','11','12') THEN 'north'
        ELSE 'south' END,
    floor = CASE
        WHEN window_no LIKE 'S2%' OR window_no IN ('04','05','06') THEN 2
        ELSE 1 END,
    location = REPLACE(REPLACE(REPLACE(location, '一楼', ''), '二楼', ''), '三楼', '');

-- ---------------------------------------------------------------------------
-- 6. 商家账号（商家端页面依赖，幂等）
-- ---------------------------------------------------------------------------
INSERT INTO sys_user (username, password, real_name, role, stall_id, status)
SELECT CONCAT(100011 + s.id), '$2a$10$9a4ogJdOUtpWZADoX3U7c.LtVttldK8A7QsqIxc6dir9UPTmhKIc.',
       CONCAT(s.name, ' 商家'), 'MERCHANT', s.id, 1
FROM canteen_stall s
WHERE s.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_user u
      WHERE u.username = CONCAT(100011 + s.id) AND u.deleted = 0
  );

-- ---------------------------------------------------------------------------
-- 7. 知识库文案同步（区域楼层 / 购物车下单，幂等）
-- ---------------------------------------------------------------------------
INSERT INTO sys_faq (category, question, answer, keywords, sort_no, status)
SELECT 'RULE', '食堂分几个区域几层楼',
       '食堂分南北两区：南区食堂有两层（1 楼、2 楼），北区食堂只有一层。南区 1 楼有川味小炒、粤式烧腊、面食工坊；南区 2 楼有麻辣香锅、清真风味、轻食沙拉；其余档口都在北区。',
       '区域,南区,北区,几楼,楼层,食堂,在哪层,几层,分布,二楼,一楼',
       0, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_faq WHERE question = '食堂分几个区域几层楼');

UPDATE sys_faq SET
  question = '怎么下单订餐',
  answer = '在「菜品浏览」点菜品卡片右下角的 + 加入购物车，或进入菜品详情页点「加入购物车」；然后点右下角悬浮球切到「购物车」页，选好档口点「结算」，填取餐时间（7 天内）和备注，提交并支付即可。订单在「我的订单」里查看。',
  keywords = '下单,订餐,购物车,加购,结算,支付,怎么买,怎么点,怎么订,提前点,留饭,预约'
WHERE category = 'ORDER' AND question = '怎么预约订餐' AND deleted = 0;

UPDATE sys_faq SET
  question = '订单可以取消吗',
  answer = '可以。在「我的订单」里找到未支付的待取餐订单，点「取消订单」即可；已支付订单请联系档口或管理员处理。',
  keywords = '取消订单,退订,不要了,怎么取消,取消预约,取消'
WHERE category = 'ORDER' AND question = '预约可以取消吗' AND deleted = 0;

UPDATE sys_faq SET question = '最多可以提前多久下单'
WHERE category = 'ORDER' AND question = '最多可以提前多久预约' AND deleted = 0;

UPDATE sys_faq SET question = '下单了没去取会怎样'
WHERE category = 'ORDER' AND question = '预约了没去取会怎样' AND deleted = 0;

SELECT '存量库结构自愈完成' AS result;
