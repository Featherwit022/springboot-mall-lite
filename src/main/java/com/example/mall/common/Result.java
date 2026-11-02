package com.example.mall.common;

public class Result<T> {
    private int code;
    private String message;
    private T data;
    private String traceId;


    public Result() {}

    private Result(int code, String message, T data) {
        this.code = code; this.message = message; this.data = data;
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
        r.setTraceId(org.slf4j.MDC.get("traceId"));
        return r;
    }

    public static <T> Result<T> ok() { return ok(null); }

    public static <T> Result<T> fail(ResultCode rc) {
        Result<T> r = new Result<>(rc.getCode(), rc.getMessage(), null);
        r.setTraceId(org.slf4j.MDC.get("traceId"));
        return r;
    }

    public static <T> Result<T> fail(ResultCode rc, String detail) {
        Result<T> r = new Result<>(rc.getCode(), detail, null);
        r.setTraceId(org.slf4j.MDC.get("traceId"));
        return r;
    }


    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
