package com.alishangtian.mtimer.processor;

import com.alishangtian.mtimer.broker.controller.BrokerStarter;
import com.alishangtian.mtimer.remoting.MtimerCommand;
import com.alishangtian.mtimer.remoting.processor.NettyRequestProcessor;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @Desc BrokerBrokerClearMetricsProcessor
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Slf4j
public class BrokerBrokerClearMetricsProcessor implements NettyRequestProcessor {
    private BrokerStarter brokerStarter;

    public BrokerBrokerClearMetricsProcessor(BrokerStarter brokerStarter) {
        this.brokerStarter = brokerStarter;
    }

    @Override
    public MtimerCommand processRequest(ChannelHandlerContext ctx, MtimerCommand request) throws Exception {
        this.brokerStarter.clearMetrics();
        return MtimerCommand.builder().result(1).build();
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
