package com.qmz.canteen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qmz.canteen.agent.AgentCardBuilder;
import com.qmz.canteen.agent.AgentOrchestrator;
import com.qmz.canteen.agent.AgentProperties;
import com.qmz.canteen.agent.LlmClient;
import com.qmz.canteen.common.BusinessException;
import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.common.ResultCode;
import com.qmz.canteen.entity.ChatLog;
import com.qmz.canteen.entity.User;
import com.qmz.canteen.mapper.ChatLogMapper;
import com.qmz.canteen.mapper.StatsMapper;
import com.qmz.canteen.mapper.UserMapper;
import com.qmz.canteen.security.LoginUser;
import com.qmz.canteen.security.UserContext;
import com.qmz.canteen.security.UserRole;
import com.qmz.canteen.service.ChatService;
import com.qmz.canteen.service.FaqService;
import com.qmz.canteen.service.StallService;
import com.qmz.canteen.service.StatsService;
import com.qmz.canteen.vo.ChatActionVO;
import com.qmz.canteen.vo.ChatAnswerVO;
import com.qmz.canteen.vo.ChatItemVO;
import com.qmz.canteen.vo.ChatLogVO;
import com.qmz.canteen.vo.ChatMissVO;
import com.qmz.canteen.vo.DishBriefVO;
import com.qmz.canteen.vo.DishScatterVO;
import com.qmz.canteen.vo.FaqVO;
import com.qmz.canteen.vo.StallPopularityVO;
import com.qmz.canteen.vo.StallVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 本地智能客服：关键词加权打分 + 数据查询，不依赖任何外部大模型。
 * 检索用的菜品/档口/知识库存内存索引，5 分钟刷新一次。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final long INDEX_TTL = 5 * 60 * 1000L;
    private static final int RATE_LIMIT = 30;
    private static final String AGENT_BUSY_TEXT =
            "助手暂时繁忙，请稍后再试。你也可以换个问法，或直接到菜品、档口页面查看。";
    private static final Pattern WINDOW_NO = Pattern.compile("(\\d{1,2})\\s*(号|窗口)");

    private static final List<IntentDef> INTENTS = List.of(
            new IntentDef("hot", "热门菜品", new String[]{"推荐", "热门", "热销", "好吃", "招牌", "必点", "吃啥",
                    "吃什么", "今天吃", "排行榜", "最好吃", "受欢迎", "人气", "top", "必吃"}),
            new IntentDef("avoid", "避雷菜品", new String[]{"避雷", "踩雷", "难吃", "别点", "不要点", "最差",
                    "黑榜", "差评", "难以下咽", "不好吃", "难吃的", "坑"}),
            new IntentDef("new", "新菜品", new String[]{"新菜", "新品", "新上架", "新出", "最近上", "新上的", "新品尝"}),
            new IntentDef("stallTop", "档口排行", new String[]{"哪个窗口", "哪个档口", "最好窗口", "档口排名",
                    "档口排行", "最受欢迎", "哪家好吃", "哪个最好", "top3", "窗口排行"}),
            new IntentDef("complaint", "投诉建议", new String[]{"投诉", "举报", "反馈", "意见", "建议", "吐槽", "不满"}),
            new IntentDef("order", "下单订餐", new String[]{"预约", "订餐", "下单", "购物车", "加购", "结算", "支付", "取餐", "留饭", "订饭", "带饭"}),
            new IntentDef("randomOrder", "随机下单", new String[]{"随机下", "随便下", "随机来", "随便来", "随机点", "随便点",
                    "随机选", "随便选", "随机整", "随便整", "来几道", "来几样", "点几道", "点几样", "下几道", "下几样",
                    "随便来几", "随机来几", "随机推荐", "随便推荐", "随机来两", "随便来两", "帮我点", "帮我下"}),
            new IntentDef("review", "评价打分", new String[]{"评价", "打分", "评分", "星级", "好评", "怎么评"}),
            new IntentDef("greeting", "打招呼", new String[]{"你好", "您好", "hi", "hello", "在吗", "嗨"}),
            new IntentDef("help", "功能说明", new String[]{"你能", "会什么", "能做什么", "帮助", "怎么用", "功能", "可以做什么"})
    );

    private final StatsMapper statsMapper;
    private final ChatLogMapper chatLogMapper;
    private final UserMapper userMapper;
    private final StatsService statsService;
    private final StallService stallService;
    private final FaqService faqService;
    /** 智能体（大模型）相关依赖 */
    private final AgentProperties agentProperties;
    private final AgentOrchestrator orchestrator;
    private final AgentCardBuilder cardBuilder;

    @Value("${canteen.good-rating-threshold}")
    private Integer goodThreshold;

    private volatile Index index;
    private volatile long indexExpireAt;

    private final Map<Long, long[]> rateBucket = new ConcurrentHashMap<>();

    @Override
    public ChatAnswerVO ask(String question, List<Map<String, String>> history) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("请先登录后再提问");
        }
        checkRate(userId);
        long start = System.currentTimeMillis();

        String raw = question == null ? "" : question.trim();
        String norm = normalize(raw);

        ChatAnswerVO answer = tryAgent(raw, history);
        if (answer == null) {
            // Agent 不可用（未配置/超时/限流/报错），降级到本地规则引擎
            answer = route(norm, raw);
        }

        ChatLog log = new ChatLog();
        log.setUserId(userId);
        log.setQuestion(raw.length() > 500 ? raw.substring(0, 500) : raw);
        log.setIntent(answer.getIntent());
        log.setIntentLabel(answer.getIntentLabel());
        log.setHit(Boolean.TRUE.equals(answer.getHit()) ? 1 : 0);
        log.setCostMs((int) (System.currentTimeMillis() - start));
        chatLogMapper.insert(log);
        answer.setLogId(log.getId());
        return answer;
    }

    @Override
    public ChatAnswerVO askStream(String question, List<Map<String, String>> history,
                                  java.util.function.Consumer<String> onDelta) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("请先登录后再提问");
        }
        checkRate(userId);
        long start = System.currentTimeMillis();

        String raw = question == null ? "" : question.trim();
        String norm = normalize(raw);

        ChatAnswerVO answer = tryAgentStream(raw, history, onDelta);
        if (answer == null) {
            // Agent 不可用，降级规则引擎；此时没有增量，一次性把完整答案推给前端
            answer = route(norm, raw);
            if (onDelta != null && answer.getText() != null) {
                onDelta.accept(answer.getText());
            }
        }

        ChatLog log = new ChatLog();
        log.setUserId(userId);
        log.setQuestion(raw.length() > 500 ? raw.substring(0, 500) : raw);
        log.setIntent(answer.getIntent());
        log.setIntentLabel(answer.getIntentLabel());
        log.setHit(Boolean.TRUE.equals(answer.getHit()) ? 1 : 0);
        log.setCostMs((int) (System.currentTimeMillis() - start));
        chatLogMapper.insert(log);
        answer.setLogId(log.getId());
        return answer;
    }

    /**
     * 走大模型智能体（流式）。返回 null 表示应降级到规则引擎。
     */
    private ChatAnswerVO tryAgentStream(String question, List<Map<String, String>> history,
                                        java.util.function.Consumer<String> onDelta) {
        if (!agentProperties.isEnabled() || question.isEmpty()) {
            return null;
        }
        if (!agentProperties.isEnableForAdmin() && UserContext.isAdmin()) {
            return null;
        }
        try {
            AgentOrchestrator.AgentResult result = orchestrator.chatStream(question, history, onDelta);
            return toAnswer(result, question);
        } catch (LlmClient.LlmException e) {
            log.warn("智能体流式调用失败，降级到规则引擎：{}", e.getMessage());
            if (!agentProperties.isFallbackToRule()) {
                ChatAnswerVO answer = base("agent_error", "智能助手繁忙", "text", false);
                answer.setText(AGENT_BUSY_TEXT);
                answer.setCanFeedback(true);
                return answer;
            }
            return null;
        } catch (Exception e) {
            log.error("智能体流式异常，降级到规则引擎", e);
            return null;
        }
    }

    /** 把编排结果转成前端契约的 ChatAnswerVO */
    private ChatAnswerVO toAnswer(AgentOrchestrator.AgentResult result, String question) {
        ChatAnswerVO answer = base("agent", "智能问答", "text", true);
        answer.setText(result.text());
        answer.setItems(cardBuilder.build(question, result.toolResults()));
        answer.setActions(buildCartActions(result));
        answer.setSources(List.of("智能助手 · 实时查询食堂数据库"));
        answer.setQuickReplies(UserContext.isMerchant()
                ? List.of("菜品在哪里添加", "学生下单后怎么处理", "怎么查看销售数据", "怎么绑定邮箱")
                : List.of("今天有什么推荐", "哪个窗口最好", "怎么投诉某个窗口"));
        answer.setCanFeedback(false);
        return answer;
    }

    /**
     * 本轮有成功的加购操作时，给答案底部补「去购物车」「去结算」按钮。
     * 仅学生账号生成；支付仍由用户自己在购物车/结算页完成。
     */
    private List<ChatActionVO> buildCartActions(AgentOrchestrator.AgentResult result) {
        LoginUser user = UserContext.get();
        if (user == null || !UserRole.STUDENT.name().equals(user.getRole())) {
            return List.of();
        }
        Object cartResult = result.toolResults() == null ? null : result.toolResults().get("add_to_cart");
        if (!(cartResult instanceof Map<?, ?> map) || !Boolean.TRUE.equals(map.get("success"))) {
            return List.of();
        }
        ChatActionVO goCart = new ChatActionVO();
        goCart.setType("link");
        goCart.setLabel("去购物车");
        goCart.setPath("/cart");
        goCart.setPrimary(true);

        ChatActionVO checkout = new ChatActionVO();
        checkout.setType("link");
        checkout.setLabel("去结算");
        checkout.setPath("/cart");
        checkout.setPrimary(false);

        return List.of(goCart, checkout);
    }

    /**
     * 走大模型智能体。返回 null 表示应降级到规则引擎。
     */
    private ChatAnswerVO tryAgent(String question, List<Map<String, String>> history) {
        if (!agentProperties.isEnabled() || question.isEmpty()) {
            return null;
        }
        if (!agentProperties.isEnableForAdmin() && UserContext.isAdmin()) {
            return null;
        }
        try {
            AgentOrchestrator.AgentResult result = orchestrator.chat(question, history);
            return toAnswer(result, question);
        } catch (LlmClient.LlmException e) {
            log.warn("智能体调用失败，降级到规则引擎：{}", e.getMessage());
            if (!agentProperties.isFallbackToRule()) {
                ChatAnswerVO answer = base("agent_error", "智能助手繁忙", "text", false);
                answer.setText(AGENT_BUSY_TEXT);
                answer.setCanFeedback(true);
                return answer;
            }
            return null;
        } catch (Exception e) {
            log.error("智能体异常，降级到规则引擎", e);
            return null;
        }
    }

    @Override
    public String engine() {
        return agentProperties.isEnabled() ? "agent" : "rule";
    }

    @Override
    public List<String> suggestions() {
        // 商家端只关心经营操作，不展示学生向问题
        if (UserContext.isMerchant()) {
            return List.of(
                    "菜品在哪里添加",
                    "怎么修改菜品价格",
                    "学生下单后怎么处理",
                    "怎么查看销售数据",
                    "怎么绑定邮箱",
                    "怎么给管理员留言"
            );
        }
        List<String> list = new ArrayList<>(List.of(
                "今天有什么推荐",
                "哪些菜要避雷",
                "怎么投诉某个窗口",
                "食堂几点营业",
                "预约了怎么取消"
        ));
        List<DishBriefVO> hot = index().dishes.stream()
                .filter(d -> d.getReviewCount() != null && d.getReviewCount() >= 8)
                .sorted(Comparator.comparing(DishBriefVO::getAvgRating, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(DishBriefVO::getReviewCount, Comparator.reverseOrder()))
                .limit(2)
                .toList();
        for (DishBriefVO d : hot) {
            list.add(d.getDishName() + "怎么样");
        }
        return list;
    }

    @Override
    public void feedback(Long logId, Integer helpful) {
        ChatLog log = chatLogMapper.selectById(logId);
        if (log == null) {
            throw new BusinessException("该记录不存在");
        }
        Long userId = UserContext.getUserId();
        if (!UserContext.isAdmin() && !log.getUserId().equals(userId)) {
            throw new BusinessException("只能评价自己的提问");
        }
        ChatLog update = new ChatLog();
        update.setId(logId);
        update.setHelpful(helpful == null || helpful == 0 ? 0 : 1);
        chatLogMapper.updateById(update);
    }

    @Override
    public PageResult<ChatLogVO> pageLogs(Long current, Long size) {
        UserContext.requireAdmin();
        IPage<ChatLog> page = chatLogMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<ChatLog>().orderByDesc(ChatLog::getId));
        List<ChatLog> records = page.getRecords() == null ? List.of() : page.getRecords();
        Map<Long, User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .select(User::getId, User::getUsername, User::getRealName)
                        .in(User::getId, records.stream().map(ChatLog::getUserId).filter(java.util.Objects::nonNull).toList()))
                .stream().collect(java.util.stream.Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        List<ChatLogVO> vos = records.stream().map(log -> {
            ChatLogVO vo = new ChatLogVO();
            vo.setId(log.getId());
            vo.setUserId(log.getUserId());
            User u = users.get(log.getUserId());
            vo.setUsername(u == null ? null : u.getUsername());
            vo.setRealName(u == null ? null : u.getRealName());
            vo.setQuestion(log.getQuestion());
            vo.setIntent(log.getIntent());
            vo.setIntentLabel(log.getIntentLabel());
            vo.setHit(log.getHit());
            vo.setCostMs(log.getCostMs());
            vo.setHelpful(log.getHelpful());
            vo.setCreateTime(log.getCreateTime());
            return vo;
        }).toList();
        PageResult<ChatLogVO> result = new PageResult<>();
        result.setRecords(vos);
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setPages(page.getPages());
        return result;
    }

    @Override
    public Map<String, Object> logStats() {
        UserContext.requireAdmin();
        Long total = chatLogMapper.selectCount(new LambdaQueryWrapper<>());
        Long hit = chatLogMapper.selectCount(new LambdaQueryWrapper<ChatLog>().eq(ChatLog::getHit, 1));
        Long helpful = chatLogMapper.selectCount(new LambdaQueryWrapper<ChatLog>().eq(ChatLog::getHelpful, 1));
        Long unhelpful = chatLogMapper.selectCount(new LambdaQueryWrapper<ChatLog>().eq(ChatLog::getHelpful, 0));
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("hitCount", hit);
        stats.put("missCount", total - hit);
        stats.put("hitRate", total == 0 ? 0 : BigDecimal.valueOf(hit).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP).doubleValue());
        stats.put("helpfulCount", helpful);
        stats.put("unhelpfulCount", unhelpful);
        stats.put("satisfaction", helpful + unhelpful == 0 ? null : BigDecimal.valueOf(helpful)
                .multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(helpful + unhelpful), 1, RoundingMode.HALF_UP).doubleValue());
        return stats;
    }

    @Override
    public List<ChatMissVO> missTop(Long limit) {
        UserContext.requireAdmin();
        return chatLogMapper.selectMissTop(limit == null ? 10 : Math.min(limit, 50)).stream().map(row -> {
            ChatMissVO vo = new ChatMissVO();
            vo.setQuestion(String.valueOf(row.get("question")));
            vo.setCount(((Number) row.get("cnt")).longValue());
            Object t = row.get("lastTime");
            vo.setLastTime(t instanceof LocalDateTime time ? time : null);
            return vo;
        }).toList();
    }

    // ---------------- 路由 ----------------

    private ChatAnswerVO route(String norm, String raw) {
        if (norm.isEmpty()) {
            return fallback("问题不能为空哦，换个说法试试？");
        }
        // 商家账号优先回答经营操作类问题
        if (UserContext.isMerchant()) {
            ChatAnswerVO merchant = answerMerchant(norm);
            if (merchant != null) {
                return merchant;
            }
        }
        List<Candidate> dishes = matchDishes(norm);
        if (!dishes.isEmpty() && dishes.get(0).score >= 5.5) {
            List<Candidate> strong = dishes.stream().filter(c -> c.score >= 5.5).toList();
            return strong.size() == 1 ? answerDish(strong.get(0).dish) : answerDishCandidates(strong);
        }
        List<StallVO> stalls = matchStalls(norm);
        if (!stalls.isEmpty()) {
            return answerStall(stalls.get(0));
        }
        FaqHit faq = matchFaq(norm);
        if (faq != null) {
            return answerFaq(faq);
        }
        IntentHit intent = matchIntent(norm);
        if (intent != null) {
            return answerByIntent(intent.code, norm);
        }
        // 越权/危险操作：礼貌拒绝，不落到「还没学会」的兜底
        if (containsAny(norm, "删除", "删掉", "退款", "退钱", "改价", "改价格", "封禁", "注销", "禁言",
                "删别人", "改别人", "查别人", "看别人", "黑别人", "刷差评")) {
            ChatAnswerVO answer = base("reject", "越权操作", "text", true);
            answer.setText("这个操作我帮不了哦，需要到对应的页面自行处理。"
                    + "我可以帮你查菜品、档口、推荐和下单这些事。");
            answer.setQuickReplies(List.of("今天有什么推荐", "宫保鸡丁多少钱", "怎么投诉某个窗口"));
            return answer;
        }
        return fallback("这个问题我还没学会，换个问法试试？");
    }

    /**
     * 商家经营问题兜底：Agent 未启用或降级时，按关键词直接给菜单入口。
     * 返回 null 表示不属于经营操作类问题，继续走通用路由。
     */
    private ChatAnswerVO answerMerchant(String norm) {
        if (containsAny(norm, "菜品", "菜单", "美食") && containsAny(norm, "添加", "新增", "新建", "上传",
                "管理", "修改", "编辑", "删除", "下架", "上架", "价格", "图片", "录入", "加菜")) {
            return merchantAnswer("merchant_dish", "菜品管理",
                    "在左侧菜单打开「菜品管理」，点右上角「＋ 新增菜品」，填写菜名、价格、分类、图片和简介后保存即可。"
                            + "列表里每道菜还能编辑、上架/下架或删除，勾选多条可以批量删除。",
                    List.of("怎么修改菜品价格", "学生下单后怎么处理", "怎么查看销售数据"));
        }
        if (containsAny(norm, "订单", "接单", "出餐", "取餐", "备餐")) {
            return merchantAnswer("merchant_order", "订单处理",
                    "在左侧「订单处理」查看学生订单，按取餐时间备餐；做好后点确认出餐，学生到店报订单号取餐。"
                            + "超时未取的订单可以联系学生或按规则取消。",
                    List.of("怎么查看销售数据", "怎么给管理员留言"));
        }
        if (containsAny(norm, "销售", "营业额", "收入", "数据", "卖得", "销量", "热销")) {
            return merchantAnswer("merchant_sales", "销售情况",
                    "左侧「销售情况」可以看到营业额、订单量和热销菜品，按日查看趋势，方便调整备菜量。",
                    List.of("学生下单后怎么处理", "菜品在哪里添加"));
        }
        if (containsAny(norm, "邮箱", "电话", "密码", "证照", "健康证", "营业执照", "头像", "资料")) {
            return merchantAnswer("merchant_profile", "账号与资料",
                    "点击右上角头像 →「个人中心」：在「基本资料」绑定或修改邮箱和电话，"
                            + "在「账号安全」修改密码，在「我的证照」上传健康证和营业执照。",
                    List.of("怎么绑定邮箱", "怎么给管理员留言"));
        }
        if (containsAny(norm, "评价", "评分", "差评", "打分")) {
            return merchantAnswer("merchant_review", "学生评价",
                    "左侧「学生评价」可以查看学生对你档口菜品的评分和评价，评分偏低时建议调整口味、分量或价格。",
                    List.of("怎么查看销售数据", "菜品在哪里添加"));
        }
        if (containsAny(norm, "留言", "管理员", "联系", "反馈", "投诉")) {
            return merchantAnswer("merchant_message", "给管理员留言",
                    "左侧「给管理员留言」可以提交经营问题、证照变更或档口信息修改，管理员会在后台回复。",
                    List.of("怎么绑定邮箱", "学生下单后怎么处理"));
        }
        if (containsAny(norm, "你好", "您好", "hi", "hello", "在吗", "嗨")) {
            return merchantAnswer("merchant_greeting", "打招呼",
                    "你好！我是商家经营助手。可以问我菜品怎么添加、订单怎么处理、销售数据在哪看、邮箱怎么绑定这些经营问题。",
                    List.of("菜品在哪里添加", "学生下单后怎么处理", "怎么查看销售数据"));
        }
        if (containsAny(norm, "你能", "会什么", "能做什么", "帮助", "怎么用", "功能", "可以做什么")) {
            return merchantAnswer("merchant_help", "商家功能说明",
                    "商家常用功能：\n1. 菜品管理：新增、编辑、上下架、删除菜品\n2. 订单处理：接单与确认出餐\n"
                            + "3. 销售情况：营业额、订单量和热销菜\n4. 学生评价：查看学生反馈\n"
                            + "5. 给管理员留言 / 个人中心绑定邮箱与证照",
                    List.of("菜品在哪里添加", "怎么查看销售数据", "怎么给管理员留言"));
        }
        return null;
    }

    private ChatAnswerVO merchantAnswer(String intent, String label, String text, List<String> quickReplies) {
        ChatAnswerVO answer = base(intent, label, "text", true);
        answer.setText(text);
        answer.setSources(List.of("商家经营指南"));
        answer.setQuickReplies(quickReplies);
        return answer;
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private ChatAnswerVO answerDish(DishBriefVO dish) {
        ChatAnswerVO answer = base("dish", "菜品查询", "dishList", true);
        String stall = dish.getWindowNo() + " 号窗口 · " + dish.getStallName();
        StringBuilder text = new StringBuilder();
        text.append("「").append(dish.getDishName()).append("」在 ").append(stall).append("。");
        text.append("\n价格 ¥").append(money(dish.getPrice()));
        if (dish.getReviewCount() != null && dish.getReviewCount() > 0) {
            text.append(" ｜ 评分 ").append(num(dish.getAvgRating())).append(" 分")
                    .append(" ｜ ").append(dish.getReviewCount()).append(" 条评价")
                    .append(" ｜ 好评率 ").append(num(dish.getGoodRate())).append("%");
        } else {
            text.append(" ｜ 暂无评价，等你来当第一个打分的人");
        }
        text.append("\n").append(hotHint(dish));

        ChatItemVO item = new ChatItemVO();
        item.setId(dish.getDishId());
        item.setTitle(dish.getDishName());
        item.setSubtitle(stall + " · " + (dish.getCategory() == null ? "菜品" : dish.getCategory()));
        item.setMeta("¥" + money(dish.getPrice()) + " ｜ " + num(dish.getAvgRating()) + " 分 ｜ "
                + (dish.getReviewCount() == null ? 0 : dish.getReviewCount()) + " 条评价");
        item.setImage(dish.getImage());
        item.setBadge(dishTag(dish));
        item.setBadgeType(badgeType(dish));
        item.setLinkLabel("查看菜品详情");
        item.setLink("/dishes/" + dish.getDishId());
        answer.setItems(List.of(item));
        answer.setText(text.toString());
        answer.setSources(List.of("菜品库 · 评价统计（实时）"));
        answer.setQuickReplies(List.of("这个档口还有什么", "今天有什么推荐", "怎么下单订餐"));
        return answer;
    }

    private ChatAnswerVO answerDishCandidates(List<Candidate> candidates) {
        ChatAnswerVO answer = base("dish", "菜品查询", "dishList", true);
        answer.setText("找到 " + candidates.size() + " 道匹配的菜，点任意一个可以看详情：");
        answer.setItems(candidates.stream().limit(6).map(c -> {
            DishBriefVO dish = c.dish;
            ChatItemVO item = new ChatItemVO();
            item.setId(dish.getDishId());
            item.setTitle(dish.getDishName());
            item.setSubtitle(dish.getWindowNo() + " 号窗口 · " + dish.getStallName());
            item.setMeta("¥" + money(dish.getPrice()) + " ｜ " + num(dish.getAvgRating()) + " 分");
            item.setImage(dish.getImage());
            item.setBadge(dishTag(dish));
            item.setBadgeType(badgeType(dish));
            item.setLinkLabel("去看看");
            item.setLink("/dishes/" + dish.getDishId());
            return item;
        }).toList());
        answer.setSources(List.of("菜品库 · 评价统计（实时）"));
        answer.setQuickReplies(List.of("今天有什么推荐", "哪些菜要避雷"));
        return answer;
    }

    private ChatAnswerVO answerStall(StallVO stall) {
        ChatAnswerVO answer = base("stall", "档口查询", "stallList", true);
        StringBuilder text = new StringBuilder();
        text.append(zoneText(stall)).append(" · ")
                .append(stall.getWindowNo()).append(" 号窗口 · ").append(stall.getName())
                .append("（").append(nullToEmpty(stall.getLocation())).append("）");
        text.append("\n主营 ").append(nullToEmpty(stall.getCategory()))
                .append(" ｜ 在售 ").append(stall.getDishCount() == null ? 0 : stall.getDishCount()).append(" 道菜")
                .append(" ｜ 评分 ").append(num(stall.getAvgRating())).append(" 分")
                .append(" ｜ 好评率 ").append(num(stall.getGoodRate())).append("%");
        text.append("\n负责人 ").append(nullToEmpty(stall.getManagerName()))
                .append("（").append(nullToEmpty(stall.getManagerPhone())).append("）");
        BigDecimal hygiene = stall.getHygieneScore();
        if (hygiene != null) {
            text.append("\n学校评定卫生分 ").append(hygiene.setScale(1, RoundingMode.HALF_UP).toPlainString())
                    .append(" 分").append(hygiene.doubleValue() < 80 ? "，低于 80 分合格线，已要求限期整改" : "，达到 80 分合格线");
        }
        if (stall.getSignatureDishes() != null && !stall.getSignatureDishes().isEmpty()) {
            text.append("\n招牌菜：").append(stall.getSignatureDishes().stream()
                    .limit(3)
                    .map(d -> d.getName() + "(¥" + money(d.getPrice()) + ")")
                    .reduce((a, b) -> a + "、").orElse(""));
        }

        ChatItemVO item = new ChatItemVO();
        item.setId(stall.getId());
        item.setTitle(stall.getName());
        item.setSubtitle(zoneText(stall) + " · " + stall.getWindowNo() + " 号窗口 · " + nullToEmpty(stall.getLocation()));
        item.setMeta(stall.getDishCount() + " 道菜 ｜ " + num(stall.getAvgRating()) + " 分 ｜ 卫生 "
                + (hygiene == null ? "-" : hygiene.setScale(1, RoundingMode.HALF_UP).toPlainString()));
        boolean warn = hygiene != null && hygiene.doubleValue() < 80;
        item.setBadge(warn ? "卫生待整改" : "卫生达标");
        item.setBadgeType(warn ? "danger" : "success");
        item.setLinkLabel("看这个档口的菜");
        item.setLink("/dishes?stallId=" + stall.getId());
        answer.setItems(List.of(item));
        answer.setText(text.toString());
        answer.setSources(List.of("档口信息 · 学校评定卫生分"));
        answer.setQuickReplies(List.of("这个档口招牌菜", "怎么下单订餐", "投诉这个窗口"));
        return answer;
    }

    private ChatAnswerVO answerByIntent(String code, String norm) {
        switch (code) {
            case "hot" -> {
                ChatAnswerVO answer = base(code, "热门菜品", "rankList", true);
                List<DishBriefVO> hot = index().dishes.stream()
                        .filter(d -> d.getReviewCount() != null && d.getReviewCount() >= 5 && d.getAvgRating() != null)
                        .sorted(Comparator.comparing(DishBriefVO::getAvgRating, Comparator.reverseOrder())
                                .thenComparing(DishBriefVO::getReviewCount, Comparator.reverseOrder()))
                        .limit(6).toList();
                answer.setText("同学们评分最高的几道菜，招牌和热门都在这里：");
                answer.setItems(toDishItems(hot));
                answer.setSources(List.of("菜品库 · 评价统计（实时）"));
                answer.setQuickReplies(List.of("哪些菜要避雷", "有什么新菜", "哪个窗口最好"));
                return answer;
            }
            case "avoid" -> {
                ChatAnswerVO answer = base(code, "避雷菜品", "rankList", true);
                List<DishScatterVO> avoid = statsService.dishScatter(500L).stream()
                        .filter(d -> "avoid".equals(d.getTag()))
                        .sorted(Comparator.comparing(DishScatterVO::getAvgRating))
                        .limit(5).toList();
                if (avoid.isEmpty()) {
                    List<DishBriefVO> low = index().dishes.stream()
                            .filter(d -> d.getAvgRating() != null && d.getAvgRating() < 3.2
                                    && d.getReviewCount() != null && d.getReviewCount() >= 5)
                            .sorted(Comparator.comparing(DishBriefVO::getAvgRating))
                            .limit(5).toList();
                    answer.setText("这几道评分偏低，点之前建议先看看评价：");
                    answer.setItems(toDishItems(low));
                } else {
                    answer.setText("这些是「美食红黑榜」里的避雷菜：评分偏低且价格偏高，建议先看看评价再决定：");
                    answer.setItems(toScatterItems(avoid));
                }
                answer.setSources(List.of("美食红黑榜 · 评价统计"));
                answer.setQuickReplies(List.of("今天有什么推荐", "怎么投诉某个窗口"));
                return answer;
            }
            case "new" -> {
                ChatAnswerVO answer = base(code, "新菜品", "rankList", true);
                List<DishScatterVO> fresh = statsService.dishScatter(500L).stream()
                        .filter(d -> "new".equals(d.getTag()))
                        .sorted(Comparator.comparing(DishScatterVO::getReviewCount))
                        .limit(5).toList();
                answer.setText(fresh.isEmpty()
                        ? "暂时没有新上架的菜品，过几天再来看看。"
                        : "这些是最近上架的新菜，评价还不多，欢迎尝鲜后打分：");
                answer.setItems(toScatterItems(fresh));
                answer.setSources(List.of("美食红黑榜 · 评价统计"));
                answer.setQuickReplies(List.of("今天有什么推荐", "怎么评价菜品"));
                return answer;
            }
            case "stallTop" -> {
                ChatAnswerVO answer = base(code, "档口排行", "rankList", true);
                List<StallPopularityVO> top = statsService.stallTop(3L);
                answer.setText("最受欢迎的三个档口，综合评分、评价数和收藏量排出来的：");
                answer.setItems(top.stream().map(s -> {
                    ChatItemVO item = new ChatItemVO();
                    item.setId(s.getStallId());
                    item.setTitle(s.getStallName());
                    item.setSubtitle(s.getWindowNo() + " 号窗口 · " + nullToEmpty(s.getLocation()));
                    item.setMeta(num(s.getAvgRating()) + " 分 ｜ " + s.getReviewCount() + " 条评价 ｜ 收藏 "
                            + s.getFavoriteCount());
                    item.setBadge("TOP" + (s.getRank() == null ? "" : s.getRank()));
                    item.setBadgeType("warning");
                    item.setLinkLabel("看这个档口");
                    item.setLink("/dishes?stallId=" + s.getStallId());
                    return item;
                }).toList());
                answer.setSources(List.of("档口人气排行 · 评价统计"));
                answer.setQuickReplies(List.of("今天有什么推荐", "哪个档口卫生分低"));
                return answer;
            }
            case "complaint" -> {
                ChatAnswerVO answer = base(code, "投诉建议", "text", true);
                answer.setText("可以到「留言反馈」提交：类型选投诉或建议，关联档口选对应窗口，写清时间窗口号和具体问题，管理员 1-3 个工作日内回复。吃到异物请保留证据并当日提交。");
                answer.setCanFeedback(true);
                answer.setQuickReplies(List.of("投诉多久回复", "吃到异物怎么办", "反馈可以匿名吗"));
                return answer;
            }
            case "randomOrder" -> {
                ChatAnswerVO answer = base(code, "随机下单", "rankList", true);
                List<DishBriefVO> pool = new ArrayList<>(index().dishes.stream()
                        .filter(d -> d.getAvgRating() != null && d.getAvgRating() >= 3.5)
                        .toList());
                if (pool.size() < 3) {
                    pool = new ArrayList<>(index().dishes);
                }
                Collections.shuffle(pool);
                List<DishBriefVO> picked = pool.stream().limit(3).toList();
                answer.setText(picked.isEmpty()
                        ? "暂时没有可推荐的菜品，稍后再来看看。"
                        : "随机给你挑了几道评分不错的菜，点卡片就能加入购物车：");
                answer.setItems(toDishItems(picked));
                answer.setSources(List.of("菜品库 · 随机推荐"));
                answer.setQuickReplies(List.of("再来几道", "今天有什么推荐", "怎么下单订餐"));
                return answer;
            }
            case "order" -> {
                ChatAnswerVO answer = base(code, "下单订餐", "text", true);
                answer.setText("在「菜品浏览」点菜品卡片右下角的 + 加入购物车，再点右下角悬浮球切到「购物车」页按档口结算：选取餐时间（7 天内）、填备注后提交并支付。订单在「我的订单」里查看，待取餐且未支付的可以自行取消。");
                answer.setQuickReplies(List.of("预约了怎么取消", "最多提前多久预约", "没去取会怎样"));
                return answer;
            }
            case "review" -> {
                ChatAnswerVO answer = base(code, "评价打分", "text", true);
                answer.setText("进入菜品详情页，在底部选星级、填写内容后提交即可，每道菜每天限评价 1 次，支持匿名。提交后 30 分钟内可在「我的评价」修改。");
                answer.setQuickReplies(List.of("评价可以匿名吗", "评价可以修改吗", "同一道菜能评价几次"));
                return answer;
            }
            case "greeting" -> {
                ChatAnswerVO answer = base(code, "打招呼", "text", true);
                answer.setText("你好！我是食堂小助手。可以问我菜品价格、档口位置、热门和避雷菜，也可以问评价、投诉、下单这些事。");
                answer.setQuickReplies(List.of("今天有什么推荐", "怎么投诉某个窗口", "食堂几点营业"));
                return answer;
            }
            case "help" -> {
                ChatAnswerVO answer = base(code, "功能说明", "text", true);
                answer.setText("我能帮你这些事：\n1. 查菜品：价格、评分、在哪个档口\n2. 查档口：区域楼层、负责人、位置、卫生分、招牌菜\n3. 推荐：热门菜、避雷菜、新菜、最受欢迎档口\n4. 答疑：评价、投诉、预约、食堂规则\n直接输入菜名或问题就行。");
                answer.setQuickReplies(List.of("今天有什么推荐", "哪些菜要避雷", "预约了怎么取消"));
                return answer;
            }
            default -> {
                return fallback("这个方向我知道，但还答不准，换个更具体的问法？");
            }
        }
    }

    private ChatAnswerVO answerFaq(FaqHit hit) {
        FaqVO faq = hit.faq;
        ChatAnswerVO answer = base("faq", "知识库", "faq", true);
        String category = CATEGORY_NAME.getOrDefault(faq.getCategory(), "常见问题");
        answer.setText("【" + category + "】" + faq.getQuestion() + "\n" + faq.getAnswer());
        answer.setSources(List.of("客服知识库 · " + category));
        answer.setQuickReplies(List.of("怎么投诉某个窗口", "食堂几点营业", "预约了怎么取消"));
        answer.setCanFeedback("FEEDBACK".equals(faq.getCategory()));
        return answer;
    }

    private ChatAnswerVO fallback(String tip) {
        ChatAnswerVO answer = base("fallback", "未命中", "fallback", false);
        answer.setText(tip + "\n可以试试这样问：「宫保鸡丁多少钱」「今天有什么推荐」「怎么投诉某个窗口」。如果还是没解决，建议直接提交留言反馈，管理员会人工回复。");
        answer.setQuickReplies(List.of("你能做什么", "今天有什么推荐", "怎么投诉某个窗口"));
        answer.setCanFeedback(true);
        return answer;
    }

    // ---------------- 匹配 ----------------

    private List<Candidate> matchDishes(String norm) {
        List<Candidate> hits = new ArrayList<>();
        for (DishBriefVO dish : index().dishes) {
            String name = normalize(dish.getDishName());
            if (name.isEmpty()) {
                continue;
            }
            double score = 0;
            if (norm.contains(name)) {
                score = 6 + Math.min(name.length(), 6) * 0.2;
            } else {
                double sim = similarity(norm, name);
                if (sim >= 0.75) {
                    score = 5.6 * sim;
                }
            }
            if (score > 0) {
                long heat = dish.getReviewCount() == null ? 0 : dish.getReviewCount();
                hits.add(new Candidate(dish, score + Math.min(heat, 40) * 0.005));
            }
        }
        return hits.stream().sorted(Comparator.comparingDouble((Candidate c) -> c.score).reversed()).toList();
    }

    private List<StallVO> matchStalls(String norm) {
        List<StallVO> hits = new ArrayList<>();
        for (StallVO stall : index().stalls) {
            String name = normalize(stall.getName());
            if (!name.isEmpty() && norm.contains(name)) {
                hits.add(stall);
                continue;
            }
            Matcher m = WINDOW_NO.matcher(norm);
            if (m.find() && stall.getWindowNo() != null
                    && String.valueOf(Integer.parseInt(m.group(1))).equals(stall.getWindowNo().replaceFirst("^0+", ""))) {
                hits.add(stall);
            }
        }
        return hits;
    }

    private FaqHit matchFaq(String norm) {
        Set<String> userGrams = bigrams(norm);
        FaqHit best = null;
        for (FaqVO faq : index().faqs) {
            double score = 0;
            String kw = faq.getKeywords() == null ? "" : faq.getKeywords();
            int kwHits = 0;
            for (String k : kw.split("[,，;；\\s]+")) {
                String nk = normalize(k);
                if (nk.length() >= 2 && norm.contains(nk)) {
                    kwHits++;
                    score += 2 + Math.min(nk.length(), 5) * 0.2;
                }
            }
            // 用「标准问法 + 关键词」的二元组集合衡量提问被覆盖的程度，容忍同义说法
            Set<String> qGrams = bigrams(normalize(faq.getQuestion() + kw));
            int common = 0;
            for (String g : qGrams) {
                if (userGrams.contains(g)) {
                    common++;
                }
            }
            double coverage = userGrams.isEmpty() ? 0 : (double) common / userGrams.size();
            score += coverage * 5;
            if ((kwHits > 0 || coverage >= 0.5) && score >= 2.5 && (best == null || score > best.score)) {
                best = new FaqHit(faq, score);
            }
        }
        return best;
    }

    private IntentHit matchIntent(String norm) {
        IntentHit best = null;
        for (IntentDef def : INTENTS) {
            double score = 0;
            for (String w : def.words()) {
                if (norm.contains(normalize(w))) {
                    score += 2;
                }
            }
            if (score >= 2 && (best == null || score > best.score)) {
                best = new IntentHit(def.code(), score);
            }
        }
        return best;
    }

    // ---------------- 工具 ----------------

    private ChatAnswerVO base(String intent, String label, String type, boolean hit) {
        ChatAnswerVO answer = new ChatAnswerVO();
        answer.setIntent(intent);
        answer.setIntentLabel(label);
        answer.setType(type);
        answer.setHit(hit);
        answer.setCanFeedback(false);
        answer.setItems(List.of());
        answer.setQuickReplies(List.of());
        answer.setActions(List.of());
        return answer;
    }

    private List<ChatItemVO> toDishItems(List<DishBriefVO> dishes) {
        return dishes.stream().map(d -> {
            ChatItemVO item = new ChatItemVO();
            item.setId(d.getDishId());
            item.setTitle(d.getDishName());
            item.setSubtitle(d.getWindowNo() + " 号窗口 · " + d.getStallName());
            item.setMeta("¥" + money(d.getPrice()) + " ｜ " + num(d.getAvgRating()) + " 分 ｜ "
                    + d.getReviewCount() + " 条评价");
            item.setImage(d.getImage());
            item.setBadge(dishTag(d));
            item.setBadgeType(badgeType(d));
            item.setLinkLabel("去看看");
            item.setLink("/dishes/" + d.getDishId());
            return item;
        }).toList();
    }

    private List<ChatItemVO> toScatterItems(List<DishScatterVO> dishes) {
        return dishes.stream().map(d -> {
            ChatItemVO item = new ChatItemVO();
            item.setId(d.getDishId());
            item.setTitle(d.getDishName());
            item.setSubtitle(d.getWindowNo() + " 号窗口 · " + d.getStallName());
            item.setMeta("¥" + money(d.getPrice()) + " ｜ " + num(d.getAvgRating()) + " 分 ｜ "
                    + d.getReviewCount() + " 条评价");
            item.setBadge(d.getTagLabel());
            item.setBadgeType("avoid".equals(d.getTag()) ? "danger" : "new".equals(d.getTag()) ? "info" : "success");
            item.setLinkLabel("去看看");
            item.setLink("/dishes/" + d.getDishId());
            return item;
        }).toList();
    }

    private String hotHint(DishBriefVO dish) {
        List<String> hints = new ArrayList<>();
        if (Integer.valueOf(1).equals(dish.getFeatured())) {
            hints.add("档口招牌菜");
        }
        Integer count = dish.getReviewCount() == null ? 0 : dish.getReviewCount().intValue();
        if (count >= 20) {
            hints.add("点单热度高，累计 " + count + " 条评价");
        } else if (count == 0) {
            hints.add("新菜，还没有人评价");
        }
        Double good = dish.getGoodRate();
        if (good != null && good >= 80) {
            hints.add("好评率 " + num(good) + "%，同学们评价不错");
        } else if (good != null && good < 50 && count >= 5) {
            hints.add("好评率只有 " + num(good) + "%，建议先看评价再决定");
        }
        if (hints.isEmpty()) {
            hints.add("中规中矩，可以试试");
        }
        return "热点提示：" + String.join("；", hints) + "。饭点（11:30-12:30）该档口人流较大。";
    }

    private String dishTag(DishBriefVO dish) {
        if (Integer.valueOf(1).equals(dish.getFeatured())) {
            return "招牌";
        }
        Double rating = dish.getAvgRating();
        if (rating == null || dish.getReviewCount() == null || dish.getReviewCount() == 0) {
            return "新菜";
        }
        return rating >= 4 ? "好评" : rating >= 3.5 ? "中评" : "待观察";
    }

    private String badgeType(DishBriefVO dish) {
        String tag = dishTag(dish);
        return switch (tag) {
            case "招牌", "好评" -> "success";
            case "新菜" -> "info";
            default -> "warning";
        };
    }

    private void checkRate(Long userId) {
        long now = System.currentTimeMillis() / 60000;
        long[] bucket = rateBucket.computeIfAbsent(userId, k -> new long[]{now, 0});
        if (bucket[0] != now) {
            bucket[0] = now;
            bucket[1] = 0;
        }
        if (bucket[1] >= RATE_LIMIT) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS.getCode(), "提问太频繁了，休息一下再问吧");
        }
        bucket[1]++;
    }

    private Index index() {
        long now = System.currentTimeMillis();
        Index current = index;
        if (current == null || now > indexExpireAt) {
            synchronized (this) {
                if (index == null || now > indexExpireAt) {
                    index = new Index(statsMapper.selectDishBrief(goodThreshold),
                            stallService.listWithStats(null, null, null),
                            faqService.listEnabled());
                    indexExpireAt = now + INDEX_TTL;
                }
            }
        }
        return index;
    }

    /**
     * 主动失效索引缓存。
     * <p>
     * 管理员改完知识库/菜品后，规则引擎原本要等最多 5 分钟才生效，
     * README 演示脚本里「改完立刻回问验证生效」那一步会翻车。
     * 由管理端写操作调用此方法即可立即刷新。
     */
    public void invalidateIndex() {
        indexExpireAt = 0;
        log.debug("客服检索索引已失效，下次提问将重建");
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}，。！？、；：\"'“”‘’（）【】《》~·]", "");
    }

    private static Set<String> bigrams(String text) {
        Set<String> grams = new java.util.HashSet<>();
        if (text.length() < 2) {
            if (!text.isEmpty()) {
                grams.add(text);
            }
            return grams;
        }
        for (int i = 0; i < text.length() - 1; i++) {
            grams.add(text.substring(i, i + 2));
        }
        return grams;
    }

    /** 最长公共子序列占比，用于菜名模糊匹配 */
    private static double similarity(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[i - 1][j - 1] + 1
                        : Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        int lcs = dp[a.length()][b.length()];
        return (double) lcs / Math.min(a.length(), b.length());
    }

    private static String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String num(Double value) {
        return value == null ? "-" : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static String nullToEmpty(String value) {
        return value == null || value.isBlank() ? "待补充" : value;
    }

    /** 「南区 2 楼」这类区域楼层文本；老数据无 area/floor 时只留有的那半 */
    private static String zoneText(StallVO stall) {
        String area = "north".equals(stall.getArea()) ? "北区" : "south".equals(stall.getArea()) ? "南区" : "";
        String floor = stall.getFloor() == null ? "" : stall.getFloor() + " 楼";
        if (area.isEmpty()) {
            return floor;
        }
        if (floor.isEmpty()) {
            return area;
        }
        return area + " " + floor;
    }

    private static final Map<String, String> CATEGORY_NAME = Map.of(
            "EVALUATE", "评价相关",
            "FEEDBACK", "投诉与反馈",
            "RULE", "食堂规则",
            "ORDER", "下单订餐",
            "OTHER", "其他问题"
    );

    private record IntentDef(String code, String label, String[] words) {
    }

    private record IntentHit(String code, double score) {
    }

    private record FaqHit(FaqVO faq, double score) {
    }

    private record Candidate(DishBriefVO dish, double score) {
    }

    private record Index(List<DishBriefVO> dishes, List<StallVO> stalls, List<FaqVO> faqs) {
    }
}
