package com.alishangtian.mtimer.remoting.exception;

/**
 * @Desc RemotingConnectException
 * @Time 2020/08/30
 * @Author alishangtian
 */
public class RemotingConnectException extends RemotingException {
    private static final long serialVersionUID = -5565366231695911316L;

    public RemotingConnectException(String addr) {
        super(addr);
    }

    public RemotingConnectException(String addr, Throwable cause) {
        super("connect to " + addr + " failed", cause);
    }
}
