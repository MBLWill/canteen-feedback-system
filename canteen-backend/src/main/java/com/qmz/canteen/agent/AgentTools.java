package com.qmz.canteen.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmz.canteen.common.BusinessException;
import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.dto.CartAddRequest;
import com.qmz.canteen.dto.OrderQuery;
import com.qmz.canteen.entity.Dish;
import com.qmz.canteen.entity.Notice;
import com.qmz.canteen.mapper.DishMapper;
import com.qmz.canteen.mapper.NoticeMapper;
import com.qmz.canteen.mapper.StatsMapper;
import com.qmz.canteen.security.LoginUser;
import com.qmz.canteen.security.UserContext;
import com.qmz.canteen.security.UserRole;
import com.qmz.canteen.service.CartService;
import com.qmz.canteen.service.FaqService;
import com.qmz.canteen.service.OrderService;
import com.qmz.canteen.service.StallService;
import com.qmz.canteen.service.StatsService;
import com.qmz.canteen.vo.AccountSummaryVO;
import com.qmz.canteen.vo.DishBriefVO;
import com.qmz.canteen.vo.DishScatterVO;
import com.qmz.canteen.vo.FaqVO;
import com.qmz.canteen.vo.OrderItemVO;
import com.qmz.canteen.vo.OrderVO;
import com.qmz.canteen.vo.StallPopularityVO;
import com.qmz.canteen.vo.StallVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 智能体可调用的「工具」集合。
 * <p>
 * 设计原则：<b>所有数字都来自数据库实时查询</b>，绝不让模型凭记忆作答。
 * 这样管理员新增/修改菜品、档口、知识库后，智能体无需重新训练即可识别。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentTools {

    /** 单次工具返回的最大条目，防止把整个库塞进 prompt */
    private static final int MAX_ITEMS = 8;

    private final StatsMapper statsMapper;
    private final StatsService statsService;
    private final StallService stallService;
    private final FaqService faqService;
    private final DishMapper dishMapper;
    private final NoticeMapper noticeMapper;
    private final CartService cartService;
    private final OrderService orderService;
    private final com.qmz.canteen.service.AuthService authService;

    @Value("${canteen.good-rating-threshold}")
    private Integer goodThreshold;

    // ---------------- 工具定义（给模型看的 schema） ----------------

    public List<Map<String, Object>> definitions() {
        return List.of(
                tool("search_dishes",
                        "按菜名或关键词搜索具体菜品，返回菜名、价格、平均评分、评价数、好评率、分类、简介、所属档口。"
                                + "只用于问某一道具体的菜（多少钱、好不好吃、在哪个窗口）。keyword 可以是菜名的一部分。",
                        Map.of(
                                "keyword", Map.of("type", "string", "description", "菜品名称或关键词，例如 宫保鸡丁 / 牛肉 / 沙拉"),
                                "limit", Map.of("type", "integer", "description", "返回条数，默认 5，最大 8")
                        ),
                        List.of("keyword")),

                tool("list_stalls",
                        "列出档口/窗口信息：区域（南区/北区）、楼层、窗口号、名称、位置、主营分类、负责人、联系电话、卫生分、"
                                + "菜品数、评分、好评率、招牌菜。问某个窗口在哪、哪个窗口好、有哪些窗口、"
                                + "某个档口还有什么菜、南区/北区/几楼有什么时用。",
                        Map.of(
                                "keyword", Map.of("type", "string", "description", "可选，按档口名/窗口号/分类过滤"),
                                "area", Map.of("type", "string", "enum", List.of("south", "north"),
                                        "description", "可选，食堂区域：south=南区 north=北区"),
                                "floor", Map.of("type", "integer", "description", "可选，楼层：1 或 2（北区固定 1）")
                        ),
                        List.of()),

                tool("list_stall_dishes",
                        "列出某个档口在售的全部菜品（菜名、价格、评分、是否招牌）。"
                                + "用户问「这家还有什么菜」「这个窗口有哪些菜」时必须用它，不要凭印象回答。",
                        Map.of(
                                "stallName", Map.of("type", "string", "description", "档口名称，例如 川味小炒"),
                                "onlyFeatured", Map.of("type", "boolean", "description", "只返回招牌菜，默认 false")
                        ),
                        List.of("stallName")),

                tool("get_rankings",
                        "获取榜单。hot=评分最高的菜、avoid=评分低且价格高的避雷菜、"
                                + "new=评价数少的新菜、stall_top=最受欢迎档口、"
                                + "random=随机推荐若干道菜（用户说「随机/随便来几道」时用）。"
                                + "用户要推荐、问红黑榜、问哪个窗口最好、要随机下几道菜时用。",
                        Map.of(
                                "type", Map.of("type", "string", "enum", List.of("hot", "avoid", "new", "stall_top", "random"),
                                        "description", "榜单类型"),
                                "limit", Map.of("type", "integer", "description", "返回条数，默认 5，最大 8")
                        ),
                        List.of("type")),

                tool("search_faq",
                        "检索食堂制度知识库：营业时间、投诉流程、预约规则、评价规则、退款、卫生标准等规则类问题。"
                                + "凡是问「几点/怎么/如何/能不能/规则/流程」且不涉及具体某道菜的，都用它。",
                        Map.of(
                                "keyword", Map.of("type", "string", "description", "用户问题或其中的关键词")
                        ),
                        List.of("keyword")),

                tool("list_notices",
                        "获取食堂最新公告，例如窗口检修、延长营业、整改通报。用户问有什么通知、最近公告时用。",
                        Map.of(
                                "limit", Map.of("type", "integer", "description", "返回条数，默认 3，最大 5")
                        ),
                        List.of()),

                tool("get_overview",
                        "获取食堂整体统计：营业档口数、在售菜品数、累计评价数、今日评价数、平均评分、"
                                + "整体好评率、留言数、待处理留言数。只用于问食堂整体情况。",
                        Map.of(), List.of()),

                tool("add_to_cart",
                        "把一道菜真实加入当前学生的购物车（写数据库）。"
                                + "用户说「加入购物车 / 帮我买 / 来一份 / 加购 / 帮我点」时必须调用本工具，"
                                + "不能只列菜或口头答应；用户要几样就分别调用几次，每次只处理一道菜。"
                                + "只加购，绝不代用户支付。dishId 优先，拿不到 id 时用 dishName。",
                        Map.of(
                                "dishId", Map.of("type", "integer",
                                        "description", "菜品ID，优先用 search_dishes 返回的 dishId"),
                                "dishName", Map.of("type", "string",
                                        "description", "菜品名称，例如 宫保鸡丁；不知道 id 时使用"),
                                "quantity", Map.of("type", "integer",
                                        "description", "份数，1-9，默认 1")
                        ),
                        List.of()),

                tool("my_orders",
                        "查询当前登录用户自己的订单（订单号、档口、取餐时间、状态、支付状态、总金额、菜品明细）。"
                                + "用户问「我的订单 / 查订单 / 我订了什么 / 订单到哪一步了」时必须调用本工具。"
                                + "只能查当前用户自己的订单，不能查别人或后台全部订单。",
                        Map.of(
                                "status", Map.of("type", "string", "enum",
                                        List.of("PENDING", "DONE", "CANCELED"),
                                        "description", "可选，按状态过滤：PENDING=待取餐 DONE=已完成 CANCELED=已取消"),
                                "limit", Map.of("type", "integer", "description", "返回条数，默认 5，最大 8")
                        ),
                        List.of()),

                tool("my_balance",
                        "查询当前登录用户的账户余额与消费统计（余额、总订单数、总消费金额、已完成订单数）。"
                                + "用户问「我的余额 / 账户余额 / 还有多少钱 / 充了多少钱」时必须调用本工具，"
                                + "不能凭记忆编造金额。",
                        Map.of(), List.of())
        );
    }

    // ---------------- 工具执行 ----------------

    /**
     * 执行工具调用。
     *
     * @param name 工具名
     * @param args 模型给出的参数（已解析成 Map）
     * @return 可直接序列化给模型的结果对象
     */
    public Object execute(String name, Map<String, Object> args) {
        Map<String, Object> safeArgs = args == null ? Map.of() : args;
        // 统一权限守卫：所有工具执行前先校验当前登录身份。
        // 未登录的工具请求直接拒绝；学生越权调用管理员/商家专属工具直接拒绝。
        Object guard = guardAccess(name);
        if (guard != null) {
            return guard;
        }
        return switch (name) {
            case "search_dishes" -> searchDishes(str(safeArgs.get("keyword")), intArg(safeArgs.get("limit"), 5));
            case "list_stalls" -> listStalls(str(safeArgs.get("keyword")), normalizeArea(str(safeArgs.get("area"))), intOrNull(safeArgs.get("floor")));
            case "list_stall_dishes" -> listStallDishes(str(safeArgs.get("stallName")), boolArg(safeArgs.get("onlyFeatured")));
            case "get_rankings" -> rankings(str(safeArgs.get("type")), intArg(safeArgs.get("limit"), 5));
            case "search_faq" -> searchFaq(str(safeArgs.get("keyword")));
            case "list_notices" -> listNotices(intArg(safeArgs.get("limit"), 3));
            case "get_overview" -> overview();
            case "add_to_cart" -> addToCart(str(safeArgs.get("dishId")), str(safeArgs.get("dishName")),
                    intArg(safeArgs.get("quantity"), 1));
            case "my_orders" -> myOrders(str(safeArgs.get("status")), intArg(safeArgs.get("limit"), 5));
            case "my_balance" -> myBalance();
            default -> Map.of("error", "未知工具：" + name);
        };
    }

    /**
     * 工具权限守卫：确保智能体无法越权。
     * <p>
     * 权限模型：
     * <ul>
     *   <li>公开查询工具（search_dishes/list_stalls/list_stall_dishes/get_rankings/
     *       search_faq/list_notices/get_overview）任何已登录用户可用；</li>
     *   <li>add_to_cart 仅学生可用（商家/管理员不能替学生加购）；</li>
     *   <li>my_orders / my_balance 仅能查「当前登录用户自己」的数据，内部用 UserContext 的 userId 强绑，无法传别人的 id；</li>
     *   <li>智能体工具列表里根本没有「删除/改价/禁用用户/查看后台」这类危险工具，从能力层面杜绝捡漏。</li>
     * </ul>
     * 返回非 null 表示拒绝（作为工具结果返回给模型，让模型如实告知用户，而不是抛异常中断对话）。
     */
    private Object guardAccess(String name) {
        LoginUser user = UserContext.get();
        if (user == null || user.getUserId() == null) {
            return Map.of("error", "请先登录后再使用该功能");
        }
        // 学生不能调用商家/管理员专属的能力；目前智能体工具集不含后台危险工具，
        // 此处是防御性兜底，防止未来新增危险工具时被学生误触。
        if ("add_to_cart".equals(name) && !UserRole.STUDENT.name().equals(user.getRole())) {
            return Map.of("success", false, "error", "只有学生账号可以使用购物车加购");
        }
        return null;
    }

    /** 按关键词搜菜：菜名 / 档口名 / 分类 / 简介都能命中，保证新加的菜也能搜到 */
    private Object searchDishes(String keyword, int limit) {
        int size = clamp(limit, 1, MAX_ITEMS);
        String kw = keyword == null ? "" : keyword.trim();

        // 直接走数据库实时查询，不依赖任何内存索引，新增数据立即可见
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStatus, 1)
                .orderByDesc(Dish::getFeatured)
                .orderByDesc(Dish::getId);
        if (StringUtils.hasText(kw)) {
            wrapper.and(w -> w.like(Dish::getName, kw)
                    .or().like(Dish::getCategory, kw)
                    .or().like(Dish::getDescription, kw));
        }
        List<Dish> dishes = dishMapper.selectList(wrapper.last("LIMIT " + size));

        // 菜名没命中时，再用全量列表做一次宽松匹配（档口名、分词）
        if (dishes.isEmpty() && StringUtils.hasText(kw)) {
            dishes = fuzzyFromAll(kw, size);
        }

        List<DishBriefVO> briefs = statsMapper.selectDishBrief(goodThreshold);
        Map<Long, DishBriefVO> byId = new LinkedHashMap<>();
        for (DishBriefVO b : briefs) {
            byId.put(b.getDishId(), b);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Dish d : dishes) {
            DishBriefVO b = byId.get(d.getId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("dishId", d.getId());
            item.put("dishName", d.getName());
            item.put("price", money(d.getPrice()));
            item.put("category", d.getCategory());
            item.put("description", d.getDescription());
            item.put("featured", Integer.valueOf(1).equals(d.getFeatured()) ? "是招牌菜" : null);
            if (b != null) {
                item.put("stallName", b.getStallName());
                item.put("windowNo", b.getWindowNo());
                item.put("avgRating", b.getAvgRating());
                item.put("reviewCount", b.getReviewCount());
                item.put("goodRate", b.getGoodRate());
            }
            result.add(item);
        }
        return Map.of(
                "keyword", kw,
                "count", result.size(),
                "dishes", result,
                "hint", result.isEmpty() ? "库中没有匹配的菜品，请如实告知用户没有找到，不要编造。" : ""
        );
    }

    /**
     * 兜底检索：模型有时会把菜名截短（如把「验证用新菜」传成「新菜」），
     * 导致 like 匹配到一堆无关的菜。这里用「原始提问」在全量菜名里做包含匹配，
     * 只要用户问题里出现了完整菜名，就能精确捞出来。
     *
     * @param rawQuestion 用户的原始提问（不是模型改写后的关键词）
     */
    public Object searchByRawQuestion(String rawQuestion, int limit) {
        int size = clamp(limit, 1, MAX_ITEMS);
        String q = normalize(rawQuestion);
        if (q.isEmpty()) {
            return Map.of("count", 0, "dishes", List.of());
        }
        List<DishBriefVO> briefs = statsMapper.selectDishBrief(goodThreshold);

        // 菜名越长越精确，按长度降序，避免「新菜」这种短名抢先命中
        List<DishBriefVO> matched = new ArrayList<>();
        for (DishBriefVO b : briefs) {
            String name = normalize(b.getDishName());
            if (!name.isEmpty() && q.contains(name)) {
                matched.add(b);
            }
        }
        matched.sort(Comparator.comparingInt((DishBriefVO b) -> -normalize(b.getDishName()).length()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (DishBriefVO b : matched.stream().limit(size).toList()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("dishId", b.getDishId());
            item.put("dishName", b.getDishName());
            item.put("price", money(b.getPrice()));
            item.put("category", b.getCategory());
            item.put("stallName", b.getStallName());
            item.put("windowNo", b.getWindowNo());
            item.put("avgRating", b.getAvgRating());
            item.put("reviewCount", b.getReviewCount());
            item.put("goodRate", b.getGoodRate());
            item.put("featured", Integer.valueOf(1).equals(b.getFeatured()) ? "是招牌菜" : null);
            result.add(item);
        }
        return Map.of("count", result.size(), "dishes", result);
    }

    /** 宽松匹配：菜名包含关键词、或关键词包含菜名、或档口名/分类包含 */
    private List<Dish> fuzzyFromAll(String kw, int size) {
        List<DishBriefVO> briefs = statsMapper.selectDishBrief(goodThreshold);
        List<Long> ids = new ArrayList<>();
        for (DishBriefVO b : briefs) {
            String name = b.getDishName() == null ? "" : b.getDishName();
            String stall = b.getStallName() == null ? "" : b.getStallName();
            String category = b.getCategory() == null ? "" : b.getCategory();
            if (name.contains(kw) || kw.contains(name) || stall.contains(kw) || category.contains(kw)) {
                ids.add(b.getDishId());
            }
            if (ids.size() >= size) {
                break;
            }
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        return dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .in(Dish::getId, ids)
                .eq(Dish::getStatus, 1));
    }

    private Object listStalls(String keyword, String area, Integer floor) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        List<StallVO> stalls = stallService.listWithStats(kw, area, floor);
        // 「13号窗口」「第13号」「13号」这类说法，SQL 的 LIKE 匹配不到 window_no='13'，
        // 这里补一次窗口号归一化，避免模型被告知「查不到」。
        if (stalls.isEmpty() && kw != null) {
            String windowNo = extractWindowNo(kw);
            if (windowNo != null) {
                stalls = stallService.listWithStats(null, area, floor).stream()
                        .filter(s -> windowNo.equals(stripLeadingZeros(s.getWindowNo())))
                        .toList();
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (StallVO s : stalls) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("stallId", s.getId());
            item.put("windowNo", s.getWindowNo());
            item.put("name", s.getName());
            item.put("area", areaLabel(s.getArea()));
            item.put("floor", s.getFloor());
            item.put("location", s.getLocation());
            item.put("category", s.getCategory());
            item.put("managerName", s.getManagerName());
            item.put("managerPhone", s.getManagerPhone());
            item.put("hygieneScore", s.getHygieneScore());
            item.put("dishCount", s.getDishCount());
            item.put("avgRating", s.getAvgRating());
            item.put("goodRate", s.getGoodRate());
            if (s.getSignatureDishes() != null && !s.getSignatureDishes().isEmpty()) {
                item.put("signatureDishes", s.getSignatureDishes().stream()
                        .limit(3)
                        .map(d -> d.getName() + "（¥" + money(d.getPrice()) + "）")
                        .toList());
            }
            result.add(item);
        }
        return Map.of("count", result.size(), "stalls", result);
    }

    private static String areaLabel(String area) {
        return "north".equals(area) ? "北区" : "south".equals(area) ? "南区" : null;
    }

    private static String normalizeArea(String area) {
        if (area == null) {
            return null;
        }
        return switch (area.trim().toLowerCase(Locale.ROOT)) {
            case "south", "南区" -> "south";
            case "north", "北区" -> "north";
            default -> null;
        };
    }

    private static Integer intOrNull(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return (int) Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 列出某档口在售的全部菜品，防止模型「凭印象」编造档口招牌菜 */
    private Object listStallDishes(String stallName, boolean onlyFeatured) {
        String name = stallName == null ? "" : stallName.trim();
        if (name.isEmpty()) {
            return Map.of("error", "缺少 stallName");
        }
        // 先按名称模糊定位档口
        List<StallVO> stalls = stallService.listWithStats(name, null, null);
        if (stalls.isEmpty()) {
            List<StallVO> all = stallService.listWithStats(null, null, null);
            stalls = all.stream()
                    .filter(s -> name.contains(s.getName()) || s.getName().contains(name)
                            || name.contains(s.getWindowNo()))
                    .toList();
        }
        if (stalls.isEmpty()) {
            return Map.of("stallName", name, "count", 0, "dishes", List.of(),
                    "hint", "没有找到该档口，请如实告知用户，不要编造。");
        }
        StallVO stall = stalls.get(0);

        List<Dish> dishes = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStallId, stall.getId())
                .eq(Dish::getStatus, 1)
                .orderByDesc(Dish::getFeatured)
                .orderByDesc(Dish::getId));

        List<DishBriefVO> briefs = statsMapper.selectDishBrief(goodThreshold);
        Map<Long, DishBriefVO> byId = new LinkedHashMap<>();
        for (DishBriefVO b : briefs) {
            byId.put(b.getDishId(), b);
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Dish d : dishes) {
            boolean featured = Integer.valueOf(1).equals(d.getFeatured());
            if (onlyFeatured && !featured) {
                continue;
            }
            DishBriefVO b = byId.get(d.getId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("dishId", d.getId());
            item.put("dishName", d.getName());
            item.put("price", money(d.getPrice()));
            item.put("featured", featured);
            if (b != null) {
                item.put("avgRating", b.getAvgRating());
                item.put("reviewCount", b.getReviewCount());
            }
            list.add(item);
        }
        return Map.of(
                "stallName", stall.getName(),
                "windowNo", stall.getWindowNo(),
                "location", stall.getLocation() == null ? "" : stall.getLocation(),
                "count", list.size(),
                "dishes", list
        );
    }

    private Object rankings(String type, int limit) {
        int size = clamp(limit, 1, MAX_ITEMS);
        String t = type == null ? "hot" : type.toLowerCase(Locale.ROOT);
        List<Map<String, Object>> list = new ArrayList<>();

        switch (t) {
            case "avoid" -> {
                List<DishScatterVO> avoid = statsService.dishScatter(500L).stream()
                        .filter(d -> "avoid".equals(d.getTag()))
                        .sorted(Comparator.comparing(DishScatterVO::getAvgRating))
                        .limit(size).toList();
                for (DishScatterVO d : avoid) {
                    list.add(dishRankItem(d.getDishId(), d.getDishName(), d.getStallName(), d.getWindowNo(),
                            d.getPrice(), d.getAvgRating(), d.getReviewCount(), d.getGoodRate(), "避雷：评分低且价格偏高"));
                }
                if (list.isEmpty()) {
                    // 兜底：样本不足时给评分最低的几道
                    statsMapper.selectDishBrief(goodThreshold).stream()
                            .filter(d -> d.getAvgRating() != null && d.getReviewCount() != null && d.getReviewCount() >= 5)
                            .sorted(Comparator.comparing(DishBriefVO::getAvgRating))
                            .limit(size)
                            .forEach(d -> list.add(dishRankItem(d.getDishId(), d.getDishName(), d.getStallName(),
                                    d.getWindowNo(), d.getPrice(), d.getAvgRating(), d.getReviewCount(),
                                    d.getGoodRate(), "评分偏低")));
                }
            }
            case "new" -> {
                List<DishScatterVO> fresh = statsService.dishScatter(500L).stream()
                        .filter(d -> "new".equals(d.getTag()))
                        .sorted(Comparator.comparing(DishScatterVO::getReviewCount))
                        .limit(size).toList();
                for (DishScatterVO d : fresh) {
                    list.add(dishRankItem(d.getDishId(), d.getDishName(), d.getStallName(), d.getWindowNo(),
                            d.getPrice(), d.getAvgRating(), d.getReviewCount(), d.getGoodRate(), "新菜品，评价还不多"));
                }
            }
            case "stall_top" -> {
                List<StallPopularityVO> top = statsService.stallTop((long) size);
                for (StallPopularityVO s : top) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("rank", s.getRank());
                    item.put("stallId", s.getStallId());
                    item.put("stallName", s.getStallName());
                    item.put("windowNo", s.getWindowNo());
                    item.put("location", s.getLocation());
                    item.put("avgRating", s.getAvgRating());
                    item.put("reviewCount", s.getReviewCount());
                    item.put("favoriteCount", s.getFavoriteCount());
                    item.put("popularityScore", s.getPopularityScore());
                    list.add(item);
                }
            }
            case "random" -> {
                // 随机推荐：优先从评分 >= 3.5 的菜里随机挑，保证「随机」但不踩雷
                List<DishBriefVO> pool = new ArrayList<>(statsMapper.selectDishBrief(goodThreshold).stream()
                        .filter(d -> d.getAvgRating() != null && d.getAvgRating() >= 3.5)
                        .toList());
                if (pool.size() < size) {
                    pool = new ArrayList<>(statsMapper.selectDishBrief(goodThreshold).stream()
                            .filter(d -> d.getAvgRating() != null)
                            .toList());
                }
                Collections.shuffle(pool);
                for (DishBriefVO d : pool.stream().limit(size).toList()) {
                    list.add(dishRankItem(d.getDishId(), d.getDishName(), d.getStallName(), d.getWindowNo(),
                            d.getPrice(), d.getAvgRating(), d.getReviewCount(), d.getGoodRate(), "随机推荐"));
                }
            }
            default -> {
                List<DishBriefVO> hot = statsMapper.selectDishBrief(goodThreshold).stream()
                        .filter(d -> d.getReviewCount() != null && d.getReviewCount() >= 5 && d.getAvgRating() != null)
                        .sorted(Comparator.comparing(DishBriefVO::getAvgRating, Comparator.reverseOrder())
                                .thenComparing(DishBriefVO::getReviewCount, Comparator.reverseOrder()))
                        .limit(size).toList();
                for (DishBriefVO d : hot) {
                    list.add(dishRankItem(d.getDishId(), d.getDishName(), d.getStallName(), d.getWindowNo(),
                            d.getPrice(), d.getAvgRating(), d.getReviewCount(), d.getGoodRate(), "评分最高"));
                }
            }
        }
        return Map.of("type", t, "count", list.size(), "items", list);
    }

    private Map<String, Object> dishRankItem(Long id, String name, String stallName, String windowNo,
                                             BigDecimal price, Double avgRating, Long reviewCount,
                                             Double goodRate, String reason) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("dishId", id);
        item.put("dishName", name);
        item.put("stallName", stallName);
        item.put("windowNo", windowNo);
        item.put("price", money(price));
        item.put("avgRating", avgRating);
        item.put("reviewCount", reviewCount);
        item.put("goodRate", goodRate);
        item.put("reason", reason);
        return item;
    }

    private Object searchFaq(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        List<FaqVO> all = faqService.listEnabled();
        List<Map<String, Object>> hits = new ArrayList<>();

        if (kw.isEmpty()) {
            all.stream().limit(10).forEach(f -> hits.add(faqItem(f)));
            return Map.of("keyword", kw, "count", hits.size(), "faqs", hits);
        }

        // 按相关度打分排序，而不是「先到先得」。
        // 否则问「评价可以修改吗」时，关键词「评价」会先命中「怎么给菜品打分评价」，
        // 模型据此回答「不可以修改」，与知识库原文相反。
        String normKw = normalize(kw);
        record Scored(FaqVO faq, double score) {
        }
        List<Scored> scored = new ArrayList<>();
        for (FaqVO faq : all) {
            String question = faq.getQuestion() == null ? "" : faq.getQuestion();
            String keywords = faq.getKeywords() == null ? "" : faq.getKeywords();
            String normQ = normalize(question);

            double score = 0;
            // 1. 问句整体高度相似（用户几乎复述了标准问法）权重最高
            score += overlap(normKw, normQ) * 10;
            // 2. 问句包含用户关键词
            if (!kw.isEmpty() && question.contains(kw)) {
                score += 6;
            }
            // 3. 关键词表精确命中
            if (!kw.isEmpty() && keywords.contains(kw)) {
                score += 4;
            }
            if (containsAny(kw, keywords)) {
                score += 2;
            }
            // 4. 关键动作词（修改/删除/取消/几点）命中问句，说明问的是同一件事
            for (String verb : new String[]{"修改", "删除", "取消", "匿名", "几次", "几点", "多久",
                    "怎么", "在哪", "为什么", "能不能", "可以"}) {
                if (kw.contains(verb) && question.contains(verb)) {
                    score += 3;
                }
            }
            if (score > 0) {
                scored.add(new Scored(faq, score));
            }
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());
        scored.stream().limit(3).forEach(s -> hits.add(faqItem(s.faq())));

        // 一条都没匹配上就返回空，让模型如实说「没找到」。
        // 不能退化成「随便给几条知识库」——否则问「某不存在的菜多少钱」时，
        // 模型会把「菜品价格在哪里看」这条当答案念出来，答非所问。
        return Map.of("keyword", kw, "count", hits.size(), "faqs", hits,
                "hint", hits.isEmpty() ? "知识库里没有匹配条目，请如实告知用户没有找到，不要用其他条目替代。" : "");
    }

    private Map<String, Object> faqItem(FaqVO faq) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("category", faq.getCategory());
        item.put("question", faq.getQuestion());
        item.put("answer", faq.getAnswer());
        return item;
    }

    private Object listNotices(int limit) {
        int size = clamp(limit, 1, 5);
        List<Notice> notices = noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, 1)
                .orderByDesc(Notice::getCreateTime)
                .last("LIMIT " + size));
        List<Map<String, Object>> list = new ArrayList<>();
        for (Notice n : notices) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", n.getTitle());
            item.put("content", n.getContent());
            item.put("createTime", n.getCreateTime() == null ? null : n.getCreateTime().toString());
            list.add(item);
        }
        return Map.of("count", list.size(), "notices", list);
    }

    private Object overview() {
        var vo = statsService.overview();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("营业档口数", vo.getStallCount());
        item.put("在售菜品数", vo.getDishCount());
        item.put("累计评价数", vo.getReviewCount());
        item.put("今日评价数", vo.getTodayReviewCount());
        item.put("平均评分", vo.getAvgRating());
        item.put("整体好评率", vo.getGoodRate());
        item.put("留言总数", vo.getFeedbackCount());
        item.put("待处理留言", vo.getPendingFeedbackCount());
        item.put("学生人数", vo.getStudentCount());
        return item;
    }

    // ---------------- 真实操作类工具 ----------------

    /**
     * 把菜品加入购物车。仅学生可用，非学生直接返回可读错误，不抛异常。
     */
    private Object addToCart(String dishIdRaw, String dishNameRaw, int quantity) {
        LoginUser user = UserContext.get();
        if (user == null || user.getUserId() == null) {
            return Map.of("success", false, "error", "请先登录后再使用购物车");
        }
        if (!UserRole.STUDENT.name().equals(user.getRole())) {
            return Map.of("success", false, "error", "只有学生账号可以使用购物车");
        }

        Dish dish = resolveDish(dishIdRaw, dishNameRaw);
        if (dish == null) {
            String target = StringUtils.hasText(dishNameRaw) ? dishNameRaw.trim()
                    : StringUtils.hasText(dishIdRaw) ? dishIdRaw.trim() : "";
            String message = target.isEmpty()
                    ? "请提供菜品名称或菜品ID"
                    : "没有找到「" + target + "」这道菜，请确认菜名或先用 search_dishes 查询";
            return Map.of("success", false, "error", message);
        }

        int qty = clamp(quantity, 1, 9);
        try {
            CartAddRequest request = new CartAddRequest();
            request.setDishId(dish.getId());
            request.setQuantity(qty);
            cartService.add(request, user.getUserId());
        } catch (BusinessException e) {
            return Map.of("success", false, "error", e.getMessage());
        } catch (Exception e) {
            log.warn("加入购物车失败 dishId={}：{}", dish.getId(), e.getMessage());
            return Map.of("success", false, "error", "加入购物车失败，请稍后再试");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("dishId", dish.getId());
        result.put("dishName", dish.getName());
        result.put("price", money(dish.getPrice()));
        result.put("quantity", qty);
        result.put("hint", "已真实加入购物车。请告诉用户已加入，并提醒到购物车/结算页自行支付，不要代用户支付。");
        return result;
    }

    /**
     * 查询当前登录用户自己的订单。强绑定 UserContext 的 userId，无法查询他人或后台订单。
     */
    private Object myOrders(String status, int limit) {
        LoginUser user = UserContext.get();
        if (user == null || user.getUserId() == null) {
            return Map.of("error", "请先登录后再查询订单");
        }
        int size = clamp(limit, 1, 8);

        OrderQuery query = new OrderQuery();
        query.setCurrent(1L);
        query.setSize((long) size);
        if ("PENDING".equals(status) || "DONE".equals(status) || "CANCELED".equals(status)) {
            query.setStatus(status);
        }
        PageResult<OrderVO> page = orderService.pageMyOrders(query, user.getUserId());

        List<Map<String, Object>> list = new ArrayList<>();
        for (OrderVO o : page.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("orderNo", o.getOrderNo());
            item.put("stallName", o.getStallName());
            item.put("windowNo", o.getWindowNo());
            item.put("pickupTime", o.getPickupTime() == null ? null : o.getPickupTime().toString());
            item.put("status", o.getStatus());
            item.put("statusLabel", o.getStatusLabel());
            item.put("payStatusLabel", o.getPayStatusLabel());
            item.put("totalAmount", money(o.getTotalAmount()));

            List<Map<String, Object>> items = new ArrayList<>();
            if (o.getItems() != null) {
                for (OrderItemVO it : o.getItems()) {
                    Map<String, Object> mi = new LinkedHashMap<>();
                    mi.put("dishName", it.getDishName());
                    mi.put("price", money(it.getPrice()));
                    mi.put("quantity", it.getQuantity());
                    mi.put("amount", money(it.getAmount()));
                    items.add(mi);
                }
            }
            item.put("items", items);
            list.add(item);
        }
        return Map.of("count", list.size(), "total", page.getTotal(), "orders", list,
                "hint", list.isEmpty() ? "当前没有符合条件的订单，请如实告知用户。" : "");
    }

    /**
     * 查询当前登录用户的账户余额与消费统计。
     */
    private Object myBalance() {
        LoginUser user = UserContext.get();
        if (user == null || user.getUserId() == null) {
            return Map.of("error", "请先登录后再查询余额");
        }
        AccountSummaryVO vo = authService.getAccountSummary(user.getUserId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("balance", money(vo.getBalance()));
        result.put("orderCount", vo.getOrderCount());
        result.put("totalSpent", money(vo.getTotalSpent()));
        result.put("completedCount", vo.getCompletedCount());
        return result;
    }

    /** 菜名精确匹配优先，模糊匹配兜底；有 dishId 时直接按 id 查 */
    private Dish resolveDish(String dishIdRaw, String dishNameRaw) {
        Long dishId = parseId(dishIdRaw);
        if (dishId != null) {
            Dish dish = dishMapper.selectById(dishId);
            if (dish != null && Integer.valueOf(1).equals(dish.getStatus())) {
                return dish;
            }
        }
        String name = dishNameRaw == null ? "" : dishNameRaw.trim();
        if (name.isEmpty()) {
            return null;
        }
        List<Dish> exact = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStatus, 1)
                .eq(Dish::getName, name));
        if (!exact.isEmpty()) {
            return exact.get(0);
        }
        List<Dish> fuzzy = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStatus, 1)
                .like(Dish::getName, name)
                .orderByDesc(Dish::getFeatured)
                .orderByAsc(Dish::getId)
                .last("LIMIT 1"));
        return fuzzy.isEmpty() ? null : fuzzy.get(0);
    }

    private static Long parseId(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return (long) Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---------------- 小工具 ----------------

    private static Map<String, Object> tool(String name, String description,
                                            Map<String, Object> properties, List<String> required) {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", Map.of(
                "type", "object",
                "properties", properties,
                "required", required
        ));
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("type", "function");
        wrapper.put("function", function);
        return wrapper;
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static int intArg(Object v, int def) {
        if (v == null) {
            return def;
        }
        try {
            return (int) Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static boolean boolArg(Object v) {
        if (v == null) {
            return false;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        return "true".equalsIgnoreCase(String.valueOf(v));
    }

    private static String money(BigDecimal v) {
        return v == null ? "-" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static boolean containsAny(String text, String keywords) {
        for (String k : keywords.split("[,，;；\\s]+")) {
            if (k.length() >= 2 && text.contains(k)) {
                return true;
            }
        }
        return false;
    }

    /** 从「13号窗口」「第13号」「窗口13」里抽出窗口号 13 */
    static String extractWindowNo(String text) {
        if (text == null) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,3})\\s*(?:号|号窗口|号窗|窗口)?").matcher(text);
        if (m.find()) {
            return stripLeadingZeros(m.group(1));
        }
        return null;
    }

    static String stripLeadingZeros(String v) {
        if (v == null) {
            return "";
        }
        String t = v.replaceFirst("^0+", "");
        return t.isEmpty() ? "0" : t;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}，。！？、；：\"'“”‘’（）【】《》~·]", "");
    }

    /** 二元组重合度，用于知识库粗排 */
    private static double overlap(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        int hit = 0;
        int total = 0;
        for (int i = 0; i < a.length() - 1; i++) {
            total++;
            if (b.contains(a.substring(i, i + 2))) {
                hit++;
            }
        }
        return total == 0 ? 0 : (double) hit / total;
    }
}
