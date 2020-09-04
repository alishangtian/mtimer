package com.alishangtian.mtimer.remoting.processor;

import com.alishangtian.mtimer.remoting.MtimerCommand;
import io.netty.channel.ChannelHandlerContext;

/**
 * @Desc NettyRequestProcessor
 * @Time 2020/08/30
 * @Author alishangtian
 */
public interface NettyRequestProcessor {
    MtimerCommand processRequest(ChannelHandlerContext ctx, MtimerCommand request)
            throws Exception;

    boolean rejectRequest();
}
