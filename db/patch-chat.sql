-- 智能客服功能升级脚本（可重复执行）
USE canteen_feedback;

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
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '智能客服知识库问答表';

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

-- 知识库初始数据按 question 去重写入，重复执行不会产生副本
INSERT INTO sys_faq (category, question, answer, keywords, sort_no, status)
SELECT * FROM (
  SELECT 'EVALUATE' AS c, '怎么给菜品打分评价' AS q,
    '进入「档口一览」找到菜品所在窗口，点进菜品详情页，在底部「写下你的评价」处选星级、填写内容，点提交即可。也可以从左侧菜单「我的评价」查看已提交的评价。' AS a,
    '打分,评价,评分,怎么评价,如何评价,写评价,评星,星级', 100, 1
  UNION ALL SELECT 'EVALUATE', '评价可以修改或删除吗',
    '进入菜品详情页提交后，可以随时在「我的评价」里重新提交来修改，也可以直接删除；删除后重新评价会恢复并覆盖原来的那条。',
    '改评价,修改评价,删除评价,撤销评价,评价错了,能改,改吗,能删,删掉评价,评价不见了', 95, 1
  UNION ALL SELECT 'EVALUATE', '评价可以匿名吗',
    '可以。提交评价时勾选「匿名评价」，其他同学就看不到你的姓名和学号，但仍会计入评分统计。',
    '匿名,匿名评价,隐藏姓名,实名吗,会被看到吗', 90, 1
  UNION ALL SELECT 'EVALUATE', '为什么我的评价没有显示',
    '评价提交后立即公开，可以在菜品详情页看到。如果内容不合适，管理员会在「评价审核」里屏蔽，被屏蔽的评价不计入评分统计。',
    '评价不见了,评价没显示,审核,评价消失,看不到评价', 85, 1
  UNION ALL SELECT 'EVALUATE', '同一道菜可以评价几次',
    '每道菜每人只保留一条评价，再次提交会覆盖上一次的评分和内容，方便你随时更新看法。',
    '评价几次,重复评价,再评价,一天一次', 80, 1
  UNION ALL SELECT 'FEEDBACK', '怎么投诉某个窗口',
    '在左侧菜单「留言反馈」点「我要留言」，类型选「投诉」，关联档口选对应窗口，写清时间、窗口号和具体问题后提交。管理员会在 1-3 个工作日内回复。',
    '投诉,举报,反馈,意见,怎么反馈,投诉窗口,服务态度', 100, 1
  UNION ALL SELECT 'FEEDBACK', '投诉多久会有回复',
    '一般 1-3 个工作日内回复，涉及食品安全的问题会当日受理。处理进度可在「留言反馈」里查看，状态会从未处理变为处理中、已解决。',
    '多久回复,什么时候回复,处理时间,进度,受理', 95, 1
  UNION ALL SELECT 'FEEDBACK', '吃到异物怎么办',
    '请先保留食物和照片，立即找档口负责人现场处理（档口卡片上有电话），同时在 24 小时内在「留言反馈」提交投诉并写清时间、窗口号和具体情况。后勤处会当日介入并给你答复。',
    '异物,头发,虫子,不新鲜,变质,吃坏了,拉肚子', 98, 1
  UNION ALL SELECT 'FEEDBACK', '反馈可以匿名吗',
    '留言反馈默认实名提交，方便管理员回访和告知处理结果；如确需匿名，请在内容开头注明「匿名提交」，我们会隐去你的姓名展示。',
    '匿名反馈,匿名留言,反馈匿名', 88, 1
  UNION ALL SELECT 'FEEDBACK', '怎么查看我的反馈处理进度',
    '在「留言反馈」页面切换到「我的」标签，可以看到自己提交的全部留言及当前状态；管理员回复后会同步显示回复内容。',
    '我的留言,查看反馈,处理进度,回复了吗,在哪看', 86, 1
  UNION ALL SELECT 'RULE', '食堂营业时间是什么时候',
    '早餐 06:30-09:00，午餐 10:30-13:30，晚餐 16:30-19:00。三楼港式茶点与饮品甜品自 11 月 1 日起延长至 18:30。节假日以食堂公告为准。',
    '营业时间,几点开门,几点关门,开放时间,什么时候有饭,还在营业吗', 100, 1
  UNION ALL SELECT 'RULE', '可以打包带走吗',
    '可以。在档口直接说明打包即可，餐具免费提供；一次性餐盒按成本收费 0.5 元。建议自带饭盒，环保又省钱。',
    '打包,带走,外带,餐盒,自带饭盒', 92, 1
  UNION ALL SELECT 'RULE', '食堂支持哪些支付方式',
    '支持校园卡刷卡、微信支付和支付宝。校园卡在一楼充值窗口办理，也可以通过校园 App 线上充值。',
    '支付,付款,刷卡,微信,支付宝,校园卡,充值', 90, 1
  UNION ALL SELECT 'RULE', '什么是小份菜和光盘行动',
    '各档口均提供「小份菜」选项，价格约为标准份的 70%，适合饭量小的同学。食堂倡导按需取餐、践行光盘，剩食称重超过 100 克会在就餐记录中提醒。',
    '光盘,小份菜,半份,浪费,吃不完,份量', 88, 1
  UNION ALL SELECT 'RULE', '清真窗口在哪里',
    '清真风味在二楼西侧 05 号窗口，设有独立操作间，提供牛羊肉专区菜品，不使用猪肉及其制品。',
    '清真,清真窗口,回族,牛肉,羊肉,在哪', 90, 1
  UNION ALL SELECT 'RULE', '卫生分是什么意思',
    '卫生分由学校后勤处每周检查评定，满分 100 分，80 分为合格线，包含操作台清洁、食材仓储、生熟分开、从业人员健康证等 12 项指标。低于 80 分的档口会在卡片上标红并限期整改。',
    '卫生分,卫生评分,卫生检查,合格,整改,干净吗', 96, 1
  UNION ALL SELECT 'RULE', '觉得菜品价格贵怎么办',
    '菜品价格由后勤处统一核定并公示。如认为定价偏高，请在「留言反馈」选择「建议」类型提交，说明菜品和建议价位，后勤处每季度会统一评估调整。',
    '价格贵,太贵了,涨价,定价,便宜点,性价比', 84, 1
  UNION ALL SELECT 'RULE', '在食堂丢了东西怎么办',
    '先到该楼层档口询问，再到一楼服务台登记失物招领；贵重物品请同步在「留言反馈」提交求助，附上丢失时间和地点，我们会协助查找。',
    '丢了,丢失,失物,招领,落下了,捡到', 82, 1
  UNION ALL SELECT 'ORDER', '怎么下单订餐',
    '在「菜品浏览」点菜品卡片右下角的 + 加入购物车，或进入菜品详情页点「加入购物车」；然后点右下角悬浮球切到「购物车」页，选好档口点「结算」，填取餐时间（7 天内）和备注，提交并支付即可。订单在「我的订单」里查看。',
    '下单,订餐,购物车,加购,结算,支付,怎么买,怎么点,怎么订,提前点,留饭,预约', 100, 1
  UNION ALL SELECT 'ORDER', '订单可以取消吗',
    '可以。在「我的订单」里找到未支付的待取餐订单，点「取消订单」即可；已支付订单请联系档口或管理员处理。',
    '取消订单,退订,不要了,怎么取消,取消预约,取消', 95, 1
  UNION ALL SELECT 'ORDER', '最多可以提前多久下单',
    '最多提前 7 天预约，取餐时间必须晚于当前时间。建议至少提前 30 分钟下单，给档口留出备餐时间。',
    '提前多久,提前几天,预约时间,最长,7天', 88, 1
  UNION ALL SELECT 'ORDER', '下单了没去取会怎样',
    '偶尔一次不影响。系统不收取费用也不会扣分，但累计 3 次未取餐会在取餐时收到提醒。如临时有事请提前取消，避免浪费。',
    '没取,不去取,爽约,放鸽子,会扣分吗', 86, 1
  UNION ALL SELECT 'OTHER', '你能做什么',
    '我可以帮你：查菜品价格、评分和所在档口；查档口负责人、位置和卫生分；推荐热门菜和避雷菜；解答评价、投诉、预约、食堂规则相关问题。直接输入菜名或问题就行。',
    '你能干嘛,能做什么,功能,帮助,怎么用,会什么', 100, 1
  UNION ALL SELECT 'OTHER', '菜品价格在哪里看',
    '在「档口一览」点进任意档口即可看到该档口全部菜品和价格；也可以直接问我菜名，比如「宫保鸡丁多少钱」。',
    '价格,多少钱,价目表,菜单,收费标准', 84, 1
) AS t
WHERE NOT EXISTS (
  SELECT 1 FROM sys_faq f WHERE f.question = t.q AND f.deleted = 0
);
