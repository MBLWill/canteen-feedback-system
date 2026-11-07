package com.qmz.canteen.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmz.canteen.vo.ChatItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把 Agent 的工具执行结果转成前端可渲染的卡片。
 * <p>
 * 模型只负责「说话」，卡片数据仍由后端从数据库结果里取，
 * 保证点进去的链接和展示的数字都是真的。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentCardBuilder {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** 默认卡片上限，避免刷屏 */
    private static final int DEFAULT_CARD_LIMIT = 6;

    /** 「三样 / 两道 / 3个 / 2份 / 四款」这类数量说法 */
    private static final Pattern QUANTITY = Pattern.compile(
            "(\\d{1,2}|一|二|两|三|四|五|六|七|八|九|十)\\s*(?:样|种|个|道|份|款)");

    /**
     * @param question    用户原始提问，用于解析「要几样」，解析不到默认 6
     * @param toolResults 工具名 → 该工具最后一次执行的原始结果对象
     */
    public List<ChatItemVO> build(String question, Map<String, Object> toolResults) {
        List<ChatItemVO> cards = new ArrayList<>();
        if (toolResults == null || toolResults.isEmpty()) {
            return cards;
        }

        Object dishResult = toolResults.get("search_dishes");
        if (dishResult != null) {
            cards.addAll(fromDishes(dishResult));
        }
        Object rankResult = toolResults.get("get_rankings");
        if (rankResult != null && cards.isEmpty()) {
            cards.addAll(fromRankings(rankResult));
        }
        Object stallResult = toolResults.get("list_stalls");
        if (stallResult != null && cards.isEmpty()) {
            cards.addAll(fromStalls(stallResult));
        }

        // 按 id 去重：模型可能把同一道菜重复列在多张卡片里
        List<ChatItemVO> unique = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (ChatItemVO card : cards) {
            if (card.getId() != null && !seen.add(card.getId())) {
                continue;
            }
            unique.add(card);
        }

        int limit = parseCardLimit(question);
        return unique.size() > limit ? new ArrayList<>(unique.subList(0, limit)) : unique;
    }

    /** 从提问里解析想要的卡片数量；解析不到默认 6，最多不超过 6 */
    static int parseCardLimit(String question) {
        if (question == null || question.isBlank()) {
            return DEFAULT_CARD_LIMIT;
        }
        Matcher m = QUANTITY.matcher(question);
        if (!m.find()) {
            return DEFAULT_CARD_LIMIT;
        }
        int n = chineseNumber(m.group(1));
        if (n <= 0) {
            return DEFAULT_CARD_LIMIT;
        }
        return Math.min(n, DEFAULT_CARD_LIMIT);
    }

    private static int chineseNumber(String raw) {
        if (raw == null || raw.isEmpty()) {
            return 0;
        }
        char c = raw.charAt(0);
        if (Character.isDigit(c)) {
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return switch (c) {
            case '一' -> 1;
            case '二', '两' -> 2;
            case '三' -> 3;
            case '四' -> 4;
            case '五' -> 5;
            case '六' -> 6;
            case '七' -> 7;
            case '八' -> 8;
            case '九' -> 9;
            case '十' -> 10;
            default -> 0;
        };
    }

    private List<ChatItemVO> fromDishes(Object result) {
        List<ChatItemVO> list = new ArrayList<>();
        JsonNode dishes = node(result, "dishes");
        if (dishes == null || !dishes.isArray()) {
            return list;
        }
        for (JsonNode d : dishes) {
            ChatItemVO item = new ChatItemVO();
            item.setId(longVal(d, "dishId"));
            item.setTitle(text(d, "dishName"));
            String windowNo = text(d, "windowNo");
            String stallName = text(d, "stallName");
            item.setSubtitle((windowNo == null ? "" : windowNo + " 号窗口 · ") + (stallName == null ? "" : stallName));
            Double rating = doubleVal(d, "avgRating");
            Long count = longVal(d, "reviewCount");
            String price = text(d, "price");
            item.setMeta("¥" + (price == null ? "-" : price)
                    + " ｜ " + (rating == null ? "暂无评分" : trimNum(rating) + " 分")
                    + " ｜ " + (count == null ? 0 : count) + " 条评价");
            String featured = text(d, "featured");
            if (featured != null && !featured.isBlank()) {
                item.setBadge("招牌");
                item.setBadgeType("success");
            } else if (rating != null && rating >= 4) {
                item.setBadge("好评");
                item.setBadgeType("success");
            } else if (rating != null && rating < 3.2) {
                item.setBadge("待观察");
                item.setBadgeType("warning");
            }
            item.setLinkLabel("查看菜品详情");
            item.setLink("/dishes/" + item.getId());
            item.setCanAddToCart(true);
            list.add(item);
        }
        return list;
    }

    private List<ChatItemVO> fromRankings(Object result) {
        List<ChatItemVO> list = new ArrayList<>();
        JsonNode items = node(result, "items");
        if (items == null || !items.isArray()) {
            return list;
        }
        for (JsonNode d : items) {
            if (!d.has("dishId")) {
                continue;
            }
            ChatItemVO item = new ChatItemVO();
            item.setId(longVal(d, "dishId"));
            item.setTitle(text(d, "dishName"));
            String windowNo = text(d, "windowNo");
            item.setSubtitle((windowNo == null ? "" : windowNo + " 号窗口 · ") + nullSafe(text(d, "stallName")));
            item.setMeta("¥" + nullSafe(text(d, "price"))
                    + " ｜ " + trimNum(doubleVal(d, "avgRating")) + " 分"
                    + " ｜ " + (longVal(d, "reviewCount") == null ? 0 : longVal(d, "reviewCount")) + " 条评价");
            String reason = text(d, "reason");
            if (reason != null) {
                item.setBadge(reason.contains("避雷") ? "避雷" : reason.contains("新") ? "新菜" : "推荐");
                item.setBadgeType(reason.contains("避雷") ? "danger" : reason.contains("新") ? "info" : "success");
            }
            item.setLinkLabel("去看看");
            item.setLink("/dishes/" + item.getId());
            item.setCanAddToCart(true);
            list.add(item);
        }
        return list;
    }

    private List<ChatItemVO> fromStalls(Object result) {
        List<ChatItemVO> list = new ArrayList<>();
        JsonNode stalls = node(result, "stalls");
        if (stalls == null || !stalls.isArray()) {
            return list;
        }
        for (JsonNode s : stalls) {
            ChatItemVO item = new ChatItemVO();
            item.setId(longVal(s, "stallId"));
            item.setTitle(text(s, "name"));
            item.setSubtitle(nullSafe(text(s, "windowNo")) + " 号窗口 · " + nullSafe(text(s, "location")));
            Double hygiene = doubleVal(s, "hygieneScore");
            item.setMeta(nullSafe(text(s, "dishCount")) + " 道菜 ｜ "
                    + trimNum(doubleVal(s, "avgRating")) + " 分 ｜ 卫生 " + (hygiene == null ? "-" : trimNum(hygiene)));
            boolean warn = hygiene != null && hygiene < 80;
            item.setBadge(warn ? "卫生待整改" : "卫生达标");
            item.setBadgeType(warn ? "danger" : "success");
            item.setLinkLabel("看这个档口的菜");
            item.setLink("/dishes?stallId=" + item.getId());
            list.add(item);
        }
        return list;
    }

    // ---------------- JSON 取值小工具 ----------------

    private JsonNode node(Object result, String field) {
        try {
            JsonNode root = JSON.valueToTree(result);
            return root.get(field);
        } catch (Exception e) {
            log.warn("卡片构建失败：{}", e.getMessage());
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static Long longVal(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asLong();
    }

    private static Double doubleVal(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asDouble();
    }

    private static String trimNum(Double v) {
        if (v == null) {
            return "-";
        }
        String s = String.format("%.2f", v);
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    private static String nullSafe(String s) {
        return s == null || s.isBlank() ? "待补充" : s;
    }
}
