package com.alishangtian.mtimer.remoting.exception;

/**
 * @Desc RemotingSendRequestException
 * @Time 2020/08/30
 * @Author alishangtian
 */
public class RemotingSendRequestException extends RemotingException {

    public RemotingSendRequestException(String addr) {
        this(addr, null);
    }

    public RemotingSendRequestException(String addr, Throwable cause) {
        super("send request to <" + addr + "> failed", cause);
    }
}
