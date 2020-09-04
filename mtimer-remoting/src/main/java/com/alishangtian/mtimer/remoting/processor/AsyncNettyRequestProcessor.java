package com.alishangtian.mtimer.remoting.processor;

import com.alishangtian.mtimer.remoting.MtimerCommand;
import com.alishangtian.mtimer.remoting.netty.RemotingResponseCallback;
import io.netty.channel.ChannelHandlerContext;

/**
 * @Desc AsyncNettyRequestProcessor
 * @Time 2020/08/30
 * @Author alishangtian
 */
public abstract class AsyncNettyRequestProcessor implements NettyRequestProcessor {

    public void asyncProcessRequest(ChannelHandlerContext ctx, MtimerCommand request, RemotingResponseCallback responseCallback) throws Exception {
        MtimerCommand response = processRequest(ctx, request);
        responseCallback.callback(response);
    }
}
