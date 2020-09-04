package com.alishangtian.mtimer.remoting.exception;

/**
 * @Desc RemotingTimeoutException
 * @Time 2020/08/30
 * @Author alishangtian
 */
public class RemotingTimeoutException extends RemotingException {

    private static final long serialVersionUID = 4106899185095245979L;

    public RemotingTimeoutException(String message) {
        super(message);
    }

    public RemotingTimeoutException(String addr, long timeoutMillis) {
        this(addr, timeoutMillis, null);
    }

    public RemotingTimeoutException(String addr, long timeoutMillis, Throwable cause) {
        super("wait response on the channel <" + addr + "> timeout, " + timeoutMillis + "(ms)", cause);
    }
}
