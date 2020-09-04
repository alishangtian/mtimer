package com.alishangtian.mtimer.remoting.exception;

/**
 * @Desc NoMoreChannelException
 * @Time 2020/08/30
 * @Author alishangtian
 */
public class NoMoreChannelException extends Exception {
    public NoMoreChannelException(String msg) {
        super(msg);
    }

    public NoMoreChannelException() {
        this("no more active channel");
    }
}
