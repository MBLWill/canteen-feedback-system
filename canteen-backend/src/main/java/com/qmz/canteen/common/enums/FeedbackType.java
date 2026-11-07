package com.qmz.canteen.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FeedbackType {

    SUGGEST("建议"),
    COMPLAINT("投诉"),
    PRAISE("表扬"),
    OTHER("其他");

    private final String label;

    public static FeedbackType of(String code) {
        if (code == null) {
            return OTHER;
        }
        for (FeedbackType type : values()) {
            if (type.name().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return OTHER;
    }
}
