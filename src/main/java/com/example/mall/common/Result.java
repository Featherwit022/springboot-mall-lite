package com.example.mall.common;

public class Result<T> {
    private int code;
    private String message;
    private T data;



    public Result() {}

    private Result(int code, String message, T data) {
        this.code = code; this.message = message; this.data = data;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), "成功", data);
    }

    public static <T> Result<T> ok() { return ok(null); }

    public static <T> Result<T> fail(ResultCode rc) {
        return new Result<>(rc.getCode(), rc.getMessage(), null);
    }

    public static <T> Result<T> fail(ResultCode rc, String detail) {
        return new Result<>(rc.getCode(), detail, null);
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
}
