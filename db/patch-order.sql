-- 订餐功能 + 档口负责人 / 招牌菜 / 卫生分字段升级脚本
-- 适用于已经执行过 schema.sql 与 init-data.sql 的库（可重复执行）
USE canteen_feedback;

-- 1. 档口负责人字段
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'canteen_stall' AND COLUMN_NAME = 'manager_name');
SET @sql := IF(@exist = 0,
    'ALTER TABLE canteen_stall ADD COLUMN manager_name VARCHAR(50) NULL COMMENT ''档口负责人姓名'' AFTER category',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'canteen_stall' AND COLUMN_NAME = 'manager_phone');
SET @sql := IF(@exist = 0,
    'ALTER TABLE canteen_stall ADD COLUMN manager_phone VARCHAR(20) NULL COMMENT ''档口负责人联系电话'' AFTER manager_name',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 档口卫生分（学校评定，0-100，低于 80 需在档口卡片公示整改）
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'canteen_stall' AND COLUMN_NAME = 'hygiene_score');
SET @sql := IF(@exist = 0,
    'ALTER TABLE canteen_stall ADD COLUMN hygiene_score DECIMAL(4,1) NOT NULL DEFAULT 85.0 COMMENT ''学校评定的卫生分（0-100），低于80需整改'' AFTER description',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 菜品招牌标记
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dish' AND COLUMN_NAME = 'featured');
SET @sql := IF(@exist = 0,
    'ALTER TABLE dish ADD COLUMN featured TINYINT NOT NULL DEFAULT 0 COMMENT ''招牌菜：1-是 0-否'' AFTER description',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 预约订餐表
CREATE TABLE IF NOT EXISTS dining_order (
    id           BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_no     VARCHAR(32)   NOT NULL COMMENT '订单号',
    user_id      BIGINT        NOT NULL COMMENT '下单学生ID',
    stall_id     BIGINT        NOT NULL COMMENT '所属档口ID',
    pickup_time  DATETIME      NOT NULL COMMENT '预约取餐时间',
    remark       VARCHAR(200)  NULL COMMENT '备注：口味、忌口等',
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '订单总金额',
    status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待取餐 DONE-已完成 CANCELED-已取消',
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user (user_id),
    KEY idx_stall (stall_id),
    KEY idx_status (status),
    KEY idx_pickup (pickup_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单主表';

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

-- 4. 补齐 9-12 号档口（已存在则跳过）
INSERT INTO canteen_stall (id, name, window_no, location, category, manager_name, manager_phone, description, status)
SELECT 9, '煲仔饭', '09', '一楼北侧', '煲仔', '梁伯贤', '18157733333', '砂锅现煲，底部带锅巴', 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM canteen_stall WHERE id = 9);
INSERT INTO canteen_stall (id, name, window_no, location, category, manager_name, manager_phone, description, status)
SELECT 10, '东北饺子', '10', '二楼北侧', '饺子', '赵桂芳', '15375850760', '现包现煮，十余种馅可选', 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM canteen_stall WHERE id = 10);
INSERT INTO canteen_stall (id, name, window_no, location, category, manager_name, manager_phone, description, status)
SELECT 11, '自选快餐', '11', '一楼南侧', '快餐', '孙立军', '15627155292', '二十余种热菜自选，米饭免费加', 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM canteen_stall WHERE id = 11);
INSERT INTO canteen_stall (id, name, window_no, location, category, manager_name, manager_phone, description, status)
SELECT 12, '铁板烧饭', '12', '三楼北侧', '铁板', '吴俊杰', '15831465041', '铁板现煎，上桌滋滋响', 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM canteen_stall WHERE id = 12);

-- 5. 补齐 9-12 号档口的菜品（按名称去重，可重复执行）
INSERT INTO dish (stall_id, name, price, category, description, featured, status)
SELECT 9, '腊味煲仔饭', 18.00, '主食', '砂锅现煲，腊味油润', 1, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dish WHERE name = '腊味煲仔饭');
INSERT INTO dish (stall_id, name, price, category, description, featured, status)
SELECT 9, '香菇滑鸡煲仔饭', 16.00, '主食', '鸡肉嫩滑，香菇入味', 0, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dish WHERE name = '香菇滑鸡煲仔饭');
INSERT INTO dish (stall_id, name, price, category, description, featured, status)
SELECT 9, '豉汁排骨煲仔饭', 17.00, '主食', '肋排铺满，米饭吸汁', 0, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dish WHERE name = '豉汁排骨煲仔饭');
INSERT INTO dish (stall_id, name, price, category, description, featured, status)
SELECT 9, '窝蛋牛肉煲仔饭', 16.00, '主食', '牛肉滑嫩，配溏心蛋', 0, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dish WHERE name = '窝蛋牛肉煲仔饭');
INSERT INTO dish (stall_id, name, price, category, description, featured, status)
SELECT 10, '猪肉白菜水饺', 12.00, '主食', '现包现煮，皮薄馅大', 1, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dish WHERE name = '猪肉白菜水饺');
INSERT INTO dish (stall_id, name, price, category, description, featured, status)
SELECT 10, '三鲜水饺', 14.00, '主食', '虾仁猪肉韭菜，鲜味足', 0, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dish WHERE name = '三鲜水饺');
INSERT INTO dish (stall_id, name, price, category, description, featured, status)
SELECT 10, '韭菜鸡蛋水饺', 11.00, '素菜', '素馅清爽，适合清淡口', 0, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dish WHERE name = '韭菜鸡蛋水饺');
INSERT INTO dish (stall_id, name, price, category, description, featured, status)
SELECT 10, '酸菜猪肉水饺', 13.00, '主食', '东北酸菜，解腻开胃', 0, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dish WHERE name = '酸菜猪肉水饺');
INSERT INTO dish (stall_id, name, price, category, description, featured, status)
SELECT 11, '两荤一素套餐', 14.00, '主食', '二十余种热菜任选，米饭免费加', 0, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dish WHERE name = '两荤一素套餐');
INSERT INTO dish (stall_id, name, price, category, description, featured, status)
SELECT 11, '一荤两素套餐', 12.00, '主食', '米饭免费加，管饱', 0, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dish WHERE name = '一荤两素套餐');
INSERT INTO dish (stall_id, name, price, category, description, featured, status)
SELECT 11, '红烧肉', 10.00, '荤菜', '肥瘦相间，入口即化', 1, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dish WHERE name = '红烧肉');
INSERT INTO dish (stall_id, name, price, category, description, featured, status)
SELECT 11, '番茄炒蛋', 6.00, '素菜', '食堂经典，下饭首选', 0, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dish WHERE name = '番茄炒蛋');
INSERT INTO dish (stall_id, name, price, category, description, featured, status)
SELECT 12, '黑椒牛柳铁板饭', 20.00, '主食', '铁板现煎，黑椒味浓', 1, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dish WHERE name = '黑椒牛柳铁板饭');
INSERT INTO dish (stall_id, name, price, category, description, featured, status)
SELECT 12, '香辣鸡腿铁板饭', 18.00, '主食', '鸡腿去骨，微辣入味', 0, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dish WHERE name = '香辣鸡腿铁板饭');
INSERT INTO dish (stall_id, name, price, category, description, featured, status)
SELECT 12, '照烧鸡排铁板饭', 17.00, '主食', '照烧汁现刷，甜咸适口', 0, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dish WHERE name = '照烧鸡排铁板饭');
INSERT INTO dish (stall_id, name, price, category, description, featured, status)
SELECT 12, '铁板时蔬豆腐', 12.00, '素菜', '素菜也能上铁板', 0, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM dish WHERE name = '铁板时蔬豆腐');

-- 6. 档口负责人 + 学校评定的卫生分
UPDATE canteen_stall SET manager_name = '张建国', manager_phone = '18335099148', hygiene_score = 92.5 WHERE id = 1;
UPDATE canteen_stall SET manager_name = '陈志明', manager_phone = '15336364674', hygiene_score = 88.0 WHERE id = 2;
UPDATE canteen_stall SET manager_name = '王秀兰', manager_phone = '18136334729', hygiene_score = 90.5 WHERE id = 3;
UPDATE canteen_stall SET manager_name = '刘德海', manager_phone = '19527624058', hygiene_score = 78.5 WHERE id = 4;
UPDATE canteen_stall SET manager_name = '马文清', manager_phone = '15278531002', hygiene_score = 86.0 WHERE id = 5;
UPDATE canteen_stall SET manager_name = '周晓雯', manager_phone = '13654131248', hygiene_score = 94.0 WHERE id = 6;
UPDATE canteen_stall SET manager_name = '黄志强', manager_phone = '17574148285', hygiene_score = 79.0 WHERE id = 7;
UPDATE canteen_stall SET manager_name = '林思颖', manager_phone = '13733124366', hygiene_score = 74.0 WHERE id = 8;
UPDATE canteen_stall SET manager_name = '梁伯贤', manager_phone = '13254039672', hygiene_score = 89.0 WHERE id = 9;
UPDATE canteen_stall SET manager_name = '赵桂芳', manager_phone = '13201092806', hygiene_score = 91.0 WHERE id = 10;
UPDATE canteen_stall SET manager_name = '孙立军', manager_phone = '19354253590', hygiene_score = 76.5 WHERE id = 11;
UPDATE canteen_stall SET manager_name = '吴俊杰', manager_phone = '18778214561', hygiene_score = 72.5 WHERE id = 12;

-- 7. 招牌菜（按名称标记，不受 ID 变动影响）
UPDATE dish SET featured = 0;
UPDATE dish SET featured = 1 WHERE name IN (
  '宫保鸡丁', '水煮肉片',
  '蜜汁叉烧饭', '烧鸭饭',
  '牛肉拉面', '山西刀削面',
  '麻辣香锅小份', '麻辣香锅大份',
  '羊肉泡馍', '新疆大盘鸡',
  '香煎鸡胸沙拉', '三文鱼牛油果沙拉',
  '水晶虾饺皇', '爆浆流沙包',
  '杨枝甘露', '顺德双皮奶',
  '腊味煲仔饭', '香菇滑鸡煲仔饭',
  '猪肉白菜水饺', '三鲜水饺',
  '红烧肉', '番茄炒蛋',
  '黑椒牛柳铁板饭', '香辣鸡腿铁板饭'
);
