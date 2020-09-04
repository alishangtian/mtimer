package com.alishangtian.mtimer.remoting.netty;

/**
 * @Desc NettyEventType
 * @Time 2020/08/30
 * @Author alishangtian
 */
public enum NettyEventType {
    /**
     * 连接
     */
    CONNECT,
    /**
     * 关闭
     */
    CLOSE,
    /**
     * 空闲
     */
    IDLE,
    /**
     * 异常
     */
    EXCEPTION
}
