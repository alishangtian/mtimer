package com.alishangtian.mtimer.client.exception;

/**
 * @Desc ClientException
 * @Time 2020/08/30
 * @Author alishangtian
 */
public class ClientException extends Exception {
    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
