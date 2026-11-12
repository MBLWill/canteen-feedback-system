# 校园食堂菜品反馈评价系统

面向高校食堂的菜品评价与意见反馈平台。学生可浏览档口菜品、打分评价、预约订餐、留言反馈；食堂管理员可维护档口菜品、审核评价、受理留言、发布公告，并通过 ECharts 数据大屏直观掌握食堂运营数据。系统内置一个**纯本地实现**的智能客服，不依赖任何外部大模型。

---

## 目录

1. [功能一览](#一功能一览)
2. [技术栈](#二技术栈)
3. [快速开始](#三快速开始三分钟跑起来)
4. [演示账号与初始数据](#四演示账号与初始数据)
5. [目录结构](#五目录结构)
6. [模块说明](#六模块说明)
7. [数据库设计](#七数据库设计)
8. [主要接口](#八主要接口)
9. [演示流程脚本](#九演示流程脚本答辩用)
10. [常见问题](#十常见问题)

---

## 一、功能一览

### 学生（STUDENT）

| 模块 | 能力 |
| --- | --- |
| 首页轮播 | 顶部轮播各窗口推广海报，点击直达该档口菜品 |
| 档口一览 | 卡片展示封面、窗口号、位置、负责人、招牌菜、评分与好评率 |
| 菜品浏览 | 关键词搜索、按档口/分类筛选、按评分/评价数/价格排序 |
| 菜品详情 | 综合评分、好评率、评分分布、最新评价、专属配图 |
| 打分评价 | 1-5 星 + 文字，支持匿名；一人一菜一评，可修改 |
| 菜品收藏 | 收藏/取消收藏，「我的收藏」集中查看 |
| 预约订餐 | 勾选菜品份数、选择取餐时间（7 天内）、填写备注，服务端计算金额 |
| 我的预约 | 查看明细与状态，待取餐的预约可自行取消 |
| 留言反馈 | 提交建议/投诉/表扬，查看管理员回复 |
| 智能客服 | 右下角悬浮窗，查菜品、查档口、看榜单、问制度 |

### 食堂管理员（ADMIN）

拥有学生全部浏览权限，另可：

- **档口管理**：增删改档口（有菜品时禁止删除），维护负责人与电话
- **菜品管理**：增删改、上下架，可标记招牌菜、配置图片地址
- **预约管理**：查看全部预约，按状态/档口/关键字筛选，标记取餐或取消
- **评价审核**：屏蔽/恢复/删除评价
- **留言处理**：回复留言并更新处理状态
- **轮播广告**：增删改首页广告，关联档口、设置跳转与排序
- **公告管理**：发布/编辑/删除食堂公告
- **用户管理**：查看用户列表，禁用/启用账号（不能操作自己）
- **客服知识库**：维护问答条目（分类、标准问法、答案、关键词、启停）
- **客服问答记录**：查看命中率、满意度、未命中 TOP10（可一键补录为知识库条目）

### 智能客服（学生端悬浮窗，规则引擎 + 大模型智能体双引擎）

系统内置**双引擎**客服，默认走智能体，失败自动降级：

| 引擎 | 说明 |
| --- | --- |
| **智能体（默认）** | 接入智谱 `glm-5.3-flash`，通过 function calling 实时查库作答 |
| **本地规则引擎（降级）** | 不依赖外部服务，断网可用；智能体超时/失败/Key 失效时自动回退 |

> 模型可切换：改 `application.yml` 的 `canteen.agent` 三行配置即可换成任意
> OpenAI 兼容服务（硅基流动 Qwen、智谱 GLM 等），代码零改动。
> 实测对比见 [docs/模型切换测试报告-GLM5.3Flash.md](docs/模型切换测试报告-GLM5.3Flash.md)。

智能体可调用的工具（全部实时查库，数字不经过模型记忆）：

| 工具 | 能力 |
| --- | --- |
| `search_dishes` | 按菜名/分类/简介搜菜，返回价格、评分、好评率、所属档口 |
| `list_stalls` | 档口列表（支持「13号窗口」这类说法归一化） |
| `list_stall_dishes` | 某档口在售的全部菜品 |
| `get_rankings` | 热门 / 避雷 / 新菜 / 最受欢迎档口 |
| `search_faq` | 制度知识库检索 |
| `list_notices` | 最新公告 |
| `get_overview` | 食堂整体统计 |

**关键特性**：

- **新增数据即时可识别**：管理员往库里加菜品/档口，无需重启、无需重训，智能体立刻能查到
- **多轮对话**：支持「它在哪个窗口」「这家还有什么菜」这类指代
- **不编造**：工具查不到就如实说没有找到；工具没返回的属性（如辣不辣）不会瞎猜
- **卡片数据来自数据库**：即使模型措辞有偏差，卡片上的价格/评分仍是真值
- **自动降级**：断网、Key 失效、超时都会自动切到规则引擎（81~576ms 响应）

配置见 `application.yml` 的 `canteen.agent`，建议用环境变量 `CANTEEN_AGENT_KEY` 覆盖 Key。
完整测试数据与踩坑记录见 [docs/智能体测试报告.md](docs/智能体测试报告.md)。

<details>
<summary>本地规则引擎能力（降级时生效）</summary>

| 能力 | 示例提问 | 数据来源 |
| --- | --- | --- |
| 菜品查询 | 「宫保鸡丁多少钱」 | 菜品库 + 评价统计 |
| 档口查询 | 「川味小炒谁负责」「05号窗口在哪」 | 档口表 |
| 榜单推荐 | 「今天有什么推荐」「哪些菜要避雷」 | 评价统计 / 红黑榜 |
| 制度问答 | 「怎么投诉某个窗口」「食堂几点营业」 | `sys_faq` 知识库 |
| 兜底 | 答不出时引导留言反馈 | — |

匹配链路：文本归一化 → 菜名/档口槽位识别 → 知识库匹配 → 关键词意图打分 → 兜底。

</details>

### ECharts 数据大屏（`/admin/dashboard`，仅管理员）

管理员登录后默认落地该页；右上角「大屏模式」可一键全屏，一屏装下核心图表，适合投屏演示。数据由后端聚合接口 `GET /stats/dashboard?days=N` 一次返回。

大屏模式是一块覆盖全屏的经典数据看板：顶部深棕横幅 + 6 项 KPI + 中部销量营收趋势通栏 + 左右两列图表 + 底部信息栏，真正铺满视口、不滚动：

| 位置 | 模块 | 类型 | 说明 |
| --- | --- | --- | --- |
| 顶部 | 核心指标 | 指标带 | 累计销量/营收、日均与当日销量、平均评分、好评率，当日销量带环比徽标 |
| 中部 | 销量与营收趋势 | 柱 + 线双轴（通栏） | 近 7/30/90 天，虚线为日均销量 |
| 左列 | 档口销量排行 · 档口经营象限 · 评分分布 | 横向条形 / 涟漪散点 / 柱状 | 档口榜可切换销量/营收并联动菜品榜 |
| 右列 | 菜品销量 TOP8 · 品类销量占比 · 各档口口碑对比 | 横向条形 / 环形 / 柱状 | 口碑大屏下为垂直柱状图，避免标签重叠 |

大屏模式直接以 `position: fixed; inset: 0` 铺满整个视口，用 CSS Grid 分配各区域高度（横幅 / KPI / 趋势 / 左右两列 / 信息栏），图表随容器自适应，任何分辨率下都不出现滚动条。普通（非全屏）模式仍是可滚动长页，保留全部 7 张图表。

数据口径基于 `dish_sales` 销量汇总表与 `dish_review` 评价表，时间基准取销量数据的最新日（演示数据可能不是当天）。

---

## 二、技术栈

| 层次 | 技术 | 版本 |
| --- | --- | --- |
| 前端 | Vue 3 + Vite + Vue Router + Pinia | Vue 3.5 / Vite 6 |
| UI | Element Plus | 2.8 |
| 图表 | ECharts | 5.5 |
| 后端 | Spring Boot | 3.5.3 |
| ORM | MyBatis-Plus | 3.5.17 |
| 运行环境 | JDK | 17 |
| 数据库 | MySQL | 8.0+ |
| 安全 | JWT（jjwt 0.12.6）+ BCrypt | - |
| 智能体 | 智谱 GLM-5.3-Flash + Function Calling（可换任意 OpenAI 兼容模型） | - |

---

## 三、快速开始（三分钟跑起来）

### 环境要求

- JDK **17**（Spring Boot 3.x 最低要求，本项目按 17 编译）
- Maven 3.8+、Node.js 18+
- MySQL 8.0+（服务需处于运行状态）

### 方式一：一键脚本（Windows，推荐）

按顺序双击项目根目录的 bat 文件：

1. `init-database.bat` — 建库建表 + 导入全部初始数据（会提示输入 MySQL root 密码，默认 123456）
2. `start-backend.bat` — 启动后端，端口 8080
3. `start-frontend.bat` — 启动前端，端口 5173（首次会自动 `npm install`）

数据库密码不是 `123456` 时，改 `canteen-backend/src/main/resources/application.yml` 里的 `spring.datasource.password`。

### 方式二：命令行

```bash
# 1. 初始化数据库（一条命令：建库 + 建表 + 全部初始数据）
mysql -uroot -p123456 --default-character-set=utf8mb4 < db/full-setup.sql

# 2. 启动后端
cd canteen-backend && mvn spring-boot:run      # http://localhost:8080/api

# 3. 启动前端（另开终端）
cd canteen-frontend && npm install && npm run dev   # http://localhost:5173
```

前端已配置代理，`/api` 请求自动转发到 8080。

### 关于数据库脚本

`db/full-setup.sql` 是**自包含的全量脚本**，把下面 6 个文件按正确顺序合并而成，一条命令即可完成建库建表灌数据（已实测：13 张表、52 道菜含配图、762 条评价、24 条客服知识库）：

`schema.sql` → `init-data.sql` → `banner-init.sql` → `patch-order.sql` → `patch-chat.sql` → `patch-dish-images.sql`

- 该文件由 `scripts/build-full-sql.py` 自动生成，改了任何子脚本后重跑一次即可
- 可重复执行：会把库**重置**为初始状态（评价等表先清空再灌入），所以演示前想恢复干净数据直接重跑
- 已有旧库、只想补功能不想清数据：单独执行对应的 `patch-*.sql`（幂等，列与记录存在时自动跳过）

---

## 四、演示账号与初始数据

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 食堂管理员 | `admin` | `admin123` |
| 食堂管理员 | `manager` | `admin123` |
| 学生 | `student01` ~ `student60` | `123456` |

初始数据：60 名学生、12 个档口、52 个菜品、762 条评价、45 条留言、164 条收藏、102 条登录日志，打开即可看到完整图表效果。52 道菜品各有一张专属配图（`canteen-frontend/public/dishes/<id>.jpg`）。

评价数据按档口口碑分层生成，差异明显，便于观察图表对比：

| 口碑档 | 档口（均分） |
| --- | --- |
| 口碑爆棚 4.3~4.5 | 川味小炒 4.45、东北饺子 4.37、麻辣香锅 4.34 |
| 口碑良好 3.8~4.0 | 煲仔饭 3.93、面食工坊 3.90、粤式烧腊 3.85、自选快餐 3.84 |
| 中规中矩 3.4~3.6 | 清真风味 3.60、港式茶点 3.44 |
| 评价偏差 2.8~3.1 | 铁板烧饭 3.05、饮品甜品 2.85 |
| 口碑垫底 | 轻食沙拉 2.58 |

整体好评率约 65%，约三成评价只打分不写文字（差评写原因比例更高），评价时间集中在饭点前后。

**学校评定的卫生分**（0-100，低于 80 需在档口卡片公示整改）与学生口碑是两套独立指标：

| 卫生分 | 档口 |
| --- | --- |
| 优秀（≥90） | 轻食沙拉 94.0、川味小炒 92.5、东北饺子 91.0、面食工坊 90.5 |
| 良好（80-90） | 煲仔饭 89.0、粤式烧腊 88.0、清真风味 86.0 |
| 待整改（<80） | 港式茶点 79.0、麻辣香锅 78.5、自选快餐 76.5、饮品甜品 74.0、铁板烧饭 72.5 |

---

## 五、目录结构

```
qimozuoye/
├── canteen-backend/                后端 Spring Boot 工程
│   ├── pom.xml
│   └── src/main/java/com/qmz/canteen/
│       ├── CanteenApplication.java 启动类
│       ├── agent/                  智能体：LLM 客户端、工具层、编排、卡片构建（6）
│       ├── common/                 统一响应、异常、枚举（7）
│       ├── config/                 MVC、MyBatis、Web 配置（4）
│       ├── controller/             REST 接口（13）
│       ├── dto/                    请求体对象（19）
│       ├── entity/                 数据库实体（13）
│       ├── mapper/                 MyBatis Mapper（14，含 XML）
│       ├── security/               JWT 工具、登录拦截器、可选登录白名单（6）
│       ├── service/                业务逻辑（15，impl 子包）
│       └── vo/                     响应视图对象（25）
│   └── src/main/resources/
│       ├── application.yml         数据源、JWT、端口、智能体配置
│       └── mapper/                 Mapper XML
│
├── canteen-frontend/               前端 Vue 3 工程
│   ├── public/dishes/              52 张菜品配图 + 1 张欢迎横幅（按菜品 id 命名）
│   ├── dist/                       前端构建产物（npm run build 生成）
│   ├── vite.config.js              端口 5173，/api 代理到 8080
│   └── src/
│       ├── api/                    13 个接口模块，统一走 request.js
│       ├── components/             轮播、菜品卡片、图表封装、客服悬浮窗等
│       ├── views/                  12 个页面（含 admin/ 下 10 个管理页）
│       ├── router/                 路由配置
│       ├── store/                  Pinia 状态（用户信息）
│       ├── utils/                  工具（菜品图兜底渐变）
│       └── styles/                 全局样式
│
├── db/                             数据库脚本
│   ├── full-setup.sql              全量一键初始化（自动合并生成，推荐用这个）
│   ├── schema.sql                  建库建表
│   ├── init-data.sql               初始业务数据
│   ├── banner-init.sql             轮播广告（建表 + 8 条数据）
│   ├── patch-order.sql             预约订餐表升级补丁
│   ├── patch-chat.sql              智能客服表 + 24 条知识库
│   ├── patch-dish-images.sql       菜品配图路径补丁
│   └── tools/                      数据生成脚本（固定随机种子，可复现）
│
├── scripts/                        工程脚本
│   ├── build-dish-images.py        菜品配图压缩（原图 → public/dishes）
│   ├── build-full-sql.py           合并生成 db/full-setup.sql
│   └── package-for-submit.py       生成可分发的项目副本
│
├── init-database.bat               一键初始化数据库
├── start-backend.bat               一键启动后端
├── start-frontend.bat              一键启动前端（自动装依赖）
└── build-frontend.bat              一键构建前端产物
```

---

## 六、模块说明

### 后端分层

| 包 | 职责 |
| --- | --- |
| `common` | `Result` 统一响应、`GlobalExceptionHandler` 全局异常处理、`BusinessException`、`ResultCode` 状态码 |
| `config` | `WebConfig`（拦截器注册与白名单）、`MybatisPlusConfig`（分页插件）、跨域配置 |
| `security` | `JwtUtil` 签发/校验、`JwtInterceptor`、`UserContext`（当前用户与管理员鉴权） |
| `controller` | 13 个控制器：认证、档口、菜品、评价、留言、公告、轮播、预约、用户、统计、客服、知识库 |
| `service` | 业务逻辑；`ChatServiceImpl` 为本地客服推理引擎（索引 5 分钟缓存 + 限流） |
| `mapper` | MyBatis-Plus 接口 + XML（统计类 SQL 集中在 `StatsMapper.xml`） |
| `dto` / `vo` | 请求体 / 响应体，BeanUtils 拷贝转换 |

### 前端要点

- `api/request.js`：axios 实例，统一携带 JWT，按 `code` 分支处理业务状态（401 跳登录）
- `components/EChart.vue`：ECharts 封装，负责实例生命周期与自适应
- `components/ChatWidget.vue`：右下角可拖拽客服悬浮窗 + `chat/AnswerBubble.vue` 答案卡片（整卡可点击跳转）
- `utils/dishImage.js`：菜品图加载失败时按分类回退渐变色 + 首字
- 路由守卫：未登录跳登录页；管理端路由 `meta.admin` 由 store 的角色控制显示

---

## 七、数据库设计

数据库 `canteen_feedback`，共 **13 张表**：

| 表名 | 说明 |
| --- | --- |
| `sys_user` | 用户表（学生 / 管理员） |
| `canteen_stall` | 食堂档口 / 窗口（含负责人、学校评定卫生分） |
| `dish` | 菜品表（含图片路径、招牌菜标记） |
| `dish_review` | 菜品评价（`dish_id + user_id` 唯一索引） |
| `feedback` | 留言反馈（含管理员回复字段） |
| `sys_notice` | 公告表 |
| `dish_favorite` | 菜品收藏（`user_id + dish_id` 唯一索引） |
| `login_log` | 登录日志 |
| `sys_banner` | 首页轮播广告（可关联档口、配置跳转与排序） |
| `dining_order` | 预约订餐主表（取餐时间、备注、总金额、状态） |
| `dining_order_item` | 预约订餐明细（菜名与单价快照、份数） |
| `sys_faq` | 智能客服知识库（分类、标准问法、答案、匹配关键词） |
| `chat_log` | 智能客服问答日志（提问、命中意图、耗时、用户评价） |

除 `chat_log` 外，其余表使用逻辑删除字段 `deleted`。

---

## 八、主要接口

```
POST /api/auth/login          登录
POST /api/auth/register       学生注册
GET  /api/auth/profile        当前用户信息

GET    /api/stalls            档口列表（含负责人、招牌菜、评分统计）
POST PUT DELETE /api/stalls   档口管理（管理员）

POST /api/orders              提交预约订餐
GET  /api/orders/my           我的预约（学生）
GET  /api/orders              全部预约（管理员）
GET  /api/orders/{id}         预约详情
PUT  /api/orders/{id}/status  状态流转

GET  /api/dishes              菜品分页（含评分统计）
GET  /api/dishes/{id}         菜品详情
GET  /api/dishes/categories   菜品分类

GET  /api/reviews             评价分页
POST /api/reviews             提交评价
GET  /api/reviews/my          我的评价
PUT  /api/reviews/{id}/status 评价状态（管理员）

GET  POST /api/feedbacks      留言列表 / 提交
PUT  /api/feedbacks/{id}/reply   回复（管理员）

GET  /api/notices             公告列表
GET  /api/banners/list        首页轮播广告（公开）
GET  /api/banners             广告分页（管理员）
POST PUT DELETE /api/banners  广告增删改（管理员）

GET  /api/stats/*             统计数据（7 个接口）
GET  /api/users               用户管理（管理员）

POST /api/chat/ask            智能客服提问（body: {question, history?}）
POST /api/chat/ask/stream     流式提问 SSE（event: delta / done / error）
GET  /api/chat/engine         当前推理引擎（agent / rule）
GET  /api/chat/suggestions    推荐问法
PUT  /api/chat/{id}/feedback  对回答打分（0/1）
GET  /api/chat/logs           问答记录（管理员）
GET  /api/chat/logs/stats     命中率与满意度统计（管理员）
GET  /api/chat/logs/miss      未命中问题 TOP（管理员）
GET  /api/faqs                知识库分页（管理员）
POST PUT DELETE /api/faqs     知识库增删改（管理员）
```

### 校验与异常处理

| 层次 | 手段 |
| --- | --- |
| 前端 | Element Plus `el-form :rules`，提交前即时提示 |
| 后端 | `jakarta.validation`（`@Valid` / `@Validated`），绕过前端同样生效 |
| 业务层 | `BusinessException` + `ResultCode`，校验存在性、权限、重复提交 |

所有异常统一转成 `{ code, message, timestamp, traceId }`（HTTP 200，业务状态由 `code` 表达）：参数校验 400、未登录 401、越权 403、不存在 404、冲突 409、兜底 500（附 8 位 `traceId` 便于查日志）。

### 预约订餐规则

- **入口**：档口卡片「预约订餐」按钮，或档口菜品页顶部按钮
- **要素**：菜品与份数（单道 1~20 份、单次最多 20 道）、取餐时间（今天起 7 天内且晚于当前时间）、备注 ≤200 字
- **金额**：服务端按当前单价重算，不信任前端传值；下单保存菜名与单价快照，后续调价不影响历史订单
- **状态**：`PENDING` 待取餐 → `DONE` 已完成 / `CANCELED` 已取消；学生只能取消自己待取餐的预约
- **校验**：菜品必须属于所选档口且在售，同一菜品不可重复，档口歇业时不允许下单

---

## 九、演示流程脚本（答辩用）

建议用**学生账号 `student01 / 123456`** 走前半段，再切换到 **`admin / admin123`** 走管理端。全程约 5-8 分钟。

**第一段：学生端浏览与互动（约 3 分钟）**

1. 登录页 → 学生账号登录，默认进入「美食探索」
2. **美食探索**：讲解档口分区（北区不分楼层 / 南区按一二楼）与菜品卡片 → 点「热销榜单」看菜品评分排行、切换周期
3. **首页轮播**：点轮播图任意一张，说明会跳转到对应档口
4. **档口一览**：指出卡片上的负责人、卫生分、招牌菜 → 点「川味小炒」进入
5. **菜品详情**：展示专属配图、评分分布 → 点星星打分 + 写一条评价（说明一人一菜一评、可匿名）
6. **预约订餐**：在档口页发起预约，勾选 2 道菜、选明天午餐时间、提交 → 到「我的预约」展示刚下的单，再演示取消

**第二段：智能客服（约 1 分钟）**

7. 点右下角悬浮球，依次问：
   - 「宫保鸡丁多少钱」→ 卡片返回档口、价格、评分（可点击卡片跳转菜品详情）
   - 「哪些菜要避雷」→ 榜单卡片
   - 「食堂几点营业」→ 知识库答案，带来源标注
8. 点「没帮助」或「有帮助」，说明数据会进 `chat_log` 供管理员分析

**第三段：管理端（约 2 分钟，换 `admin / admin123`）**

9. **客服问答记录**：展示命中率、满意度、未命中 TOP10 → 点「补录为问答」演示闭环
10. **客服知识库**：新增一条问答（如「周末营业吗」），回客服窗口再问一次，验证立刻生效
11. **评价审核**：屏蔽一条评价，回菜品详情页确认已隐藏
12. **留言处理**：回复一条留言并标记处理完成
13. **菜品管理**：标记一个招牌菜 → 回到档口页看封面是否换成该菜配图
14. **用户管理**：禁用一个学生账号（说明不能操作自己）

**收尾**：切换到 `admin` 账号，进入**数据大屏**（顶栏可点「大屏模式」一屏演示），说明所有图表数据实时来源于上述操作，形成「评价 → 统计 → 决策」闭环。

---

## 十、常见问题

| 现象 | 原因与解决 |
| --- | --- |
| IDEA 报「不支持发行版本 21」 | Project SDK 选成了旧 JDK，`File → Project Structure → Project → SDK` 切到 17 |
| 后端启动报数据库连接失败 | MySQL 服务未启动，或 `application.yml` 的 `password` 与本机 root 密码不一致 |
| 前端页面空白 / 接口 404 | 后端未启动；前端 `/api` 依赖 8080 代理 |
| `npm install` 很慢或失败 | 换国内源：`npm config set registry https://registry.npmmirror.com` |
| 端口被占用 | 改 `application.yml` 的 `server.port`，同步改 `vite.config.js` 的 proxy target |
| 菜品图不显示 | 确认 `canteen-frontend/public/dishes/` 下图片存在；缺失时会自动回退为分类渐变 + 首字 |
| 学生头像 / 档口门面图不显示 | 早期导出的数据里 `sys_user.avatar`、`canteen_stall.cover_image` 是空的（图片路径没有随数据一起提交）。后端启动时会自动执行 `db/migration/V10__seed_images.sql` 回填，重启一次后端即可；也可手动执行该 SQL |
| 数据表缺少客服表 / 预约表 | 重跑 `db/full-setup.sql` 一键对齐（会重置为初始数据）；不想清数据就补跑对应的 `db/patch-*.sql` |
| 接口返回 429 | 智能客服限流 30 次/分钟，稍等再试 |

---

## 附：数据重新生成

`db/tools/` 下是 `init-data.sql` 的生成脚本：`gen_data.cjs` 造模拟数据（固定随机种子 20261022，数据可复现）、`gen_hash.cjs` 生成 BCrypt 密码哈希。修改学生数、评价数等参数后重新执行即可覆盖 `db/init-data.sql`。

菜品配图的原始高清图不随分发包提供，`public/dishes/` 里的 768×768 压缩版（单张约 120KB）已足够使用。
