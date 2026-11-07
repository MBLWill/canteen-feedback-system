package com.qmz.canteen.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FeedbackStatus {

    PENDING("待处理"),
    PROCESSING("处理中"),
    RESOLVED("已解决");

    private final String label;

    public static FeedbackStatus of(String code) {
        if (code == null) {
            return PENDING;
        }
        for (FeedbackStatus status : values()) {
            if (status.name().equalsIgnoreCase(code)) {
                return status;
            }
        }
        return PENDING;
    }
}
