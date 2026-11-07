package com.qmz.canteen.vo;

import lombok.Data;

import java.util.List;

/**
 * 客服统一答案结构。前端只做渲染，不参与判读。
 * 若将来接入大模型，只需替换后端推理层，保持该契约不变即可。
 */
@Data
public class ChatAnswerVO {

    private Long logId;

    private String intent;

    private String intentLabel;

    private Boolean hit;

    /** text-纯文本 dishList-菜品卡 stallList-档口卡 rankList-榜单 faq-知识库 fallback-兜底 */
    private String type;

    private String text;

    private List<ChatItemVO> items;

    private List<String> sources;

    private List<String> quickReplies;

    /** 答案底部操作按钮，例如「去购物车」「去结算」，可为空 */
    private List<ChatActionVO> actions;

    /** 是否显示「去留言反馈」按钮 */
    private Boolean canFeedback;
}
