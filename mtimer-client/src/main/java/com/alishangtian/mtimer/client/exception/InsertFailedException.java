package com.alishangtian.mtimer.client.exception;

/**
 * @Desc InsertFailedException
 * @Time 2020/08/30
 * @Author alishangtian
 */
public class InsertFailedException extends Exception {
    public InsertFailedException(String message) {
        super(message);
    }

    public InsertFailedException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
