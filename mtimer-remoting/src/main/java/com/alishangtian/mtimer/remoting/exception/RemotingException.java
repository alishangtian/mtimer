package com.alishangtian.mtimer.remoting.exception;

/**
 * @Desc RemotingException
 * @Time 2020/08/30
 * @Author alishangtian
 */
public class RemotingException extends Exception {

    public RemotingException(String message) {
        super(message);
    }

    public RemotingException(String message, Throwable cause) {
        super(message, cause);
    }
}
