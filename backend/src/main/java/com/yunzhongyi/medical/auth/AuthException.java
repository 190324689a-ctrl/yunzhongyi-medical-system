package com.yunzhongyi.medical.auth;

/**
 * 鉴权异常：由全局异常处理转成统一 Result 返回。
 */
public class AuthException extends RuntimeException {

    private final int code;

    public AuthException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
