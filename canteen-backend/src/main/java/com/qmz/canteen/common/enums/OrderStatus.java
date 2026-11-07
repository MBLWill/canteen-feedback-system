package com.qmz.canteen.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatus {

    PENDING("待取餐"),
    DONE("已完成"),
    CANCELED("已取消");

    private final String label;

    public static OrderStatus of(String code) {
        if (code == null) {
            return null;
        }
        for (OrderStatus status : values()) {
            if (status.name().equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }
}
