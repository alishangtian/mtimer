package com.alishangtian.mtimer.remoting.exception;

/**
 * @Desc RemotingCommandException
 * @Time 2020/08/30
 * @Author alishangtian
 */
public class RemotingCommandException extends RemotingException {

    public RemotingCommandException(String message) {
        super(message, null);
    }

    public RemotingCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
