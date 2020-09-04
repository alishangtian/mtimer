package com.alishangtian.mtimer.processor;

import com.alishangtian.mtimer.broker.controller.BrokerStarter;
import com.alishangtian.mtimer.remoting.MtimerCommand;
import com.alishangtian.mtimer.remoting.processor.NettyRequestProcessor;
import io.netty.channel.ChannelHandlerContext;

/**
 * @Desc ClientHeartBeatToFollowerProcessor
 * @Time 2020/08/30
 * @Author alishangtian
 */
public class ClientHeartBeatToFollowerProcessor implements NettyRequestProcessor {
    private BrokerStarter brokerStarter;

    public ClientHeartBeatToFollowerProcessor(BrokerStarter brokerStarter) {
        this.brokerStarter = brokerStarter;
    }

    @Override
    public MtimerCommand processRequest(ChannelHandlerContext ctx, MtimerCommand request) throws Exception {
        return null;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
