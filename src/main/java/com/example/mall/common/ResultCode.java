package com.example.mall.common;

public enum ResultCode {
    SUCCESS(0,       "成功"),
    PARAM_ERROR(4000, "参数不合法"),
    UNAUTHORIZED(4010, "未登录"),
    NOT_FOUND(4004,  "资源不存在"),
    BIZ_ERROR(4100,  "业务处理失败"),   // 库存不足、状态冲突等
    SYSTEM_ERROR(5000, "系统繁忙");      // 兜底，绝不把堆栈给前端

    private final int code;
    private final String message;
    ResultCode(int code, String message) { this.code = code; this.message = message; }
    public int getCode() { return code; }
    public String getMessage() { return message; }
}