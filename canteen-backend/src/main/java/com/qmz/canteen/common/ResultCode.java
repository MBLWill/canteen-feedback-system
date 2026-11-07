package com.qmz.canteen.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有权限执行该操作"),
    PARAM_ERROR(400, "参数错误"),
    NOT_FOUND(404, "数据不存在"),
    DATA_EXISTS(409, "数据已存在"),
    TOO_MANY_REQUESTS(429, "操作太频繁，请稍后再试");

    private final Integer code;
    private final String message;
}
