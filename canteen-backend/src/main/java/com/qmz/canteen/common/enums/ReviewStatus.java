package com.qmz.canteen.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReviewStatus {

    PENDING("待审核", 0),
    PUBLISHED("已发布", 1),
    BLOCKED("已屏蔽", 2);

    private final String label;
    private final Integer code;

    public static ReviewStatus of(Integer code) {
        if (code == null) {
            return PUBLISHED;
        }
        for (ReviewStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return PUBLISHED;
    }
}
