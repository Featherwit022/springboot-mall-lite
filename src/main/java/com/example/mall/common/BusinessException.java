package com.example.mall.common;

/**
 * 业务异常：代表"预期内"的失败，由 GlobalExceptionHandler 统一翻译成 Result。
 * <p>
 * 默认错误码是 {@link ResultCode#BIZ_ERROR}（4100，库存不足、状态冲突等）；
 * 需要更精确的语义时（例如查不到资源用 4004），用带 ResultCode 的构造器。
 */
public class BusinessException extends RuntimeException {

    private final ResultCode code;

    public BusinessException(String message) {
        this(ResultCode.BIZ_ERROR, message);
    }

    public BusinessException(ResultCode code, String message) {
        super(message);
        this.code = code;
    }

    public ResultCode getCode() {
        return code;
    }
}
