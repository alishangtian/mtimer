package com.alishangtian.mtimer.client.exception;

/**
 * @Desc ParamException
 * @Time 2020/08/30
 * @Author alishangtian
 */
public class ParamException extends Exception {
    public ParamException(String message) {
        super(message);
    }

    public ParamException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
