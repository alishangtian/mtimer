package com.alishangtian.mtimer.processor;

import com.alishangtian.mtimer.broker.controller.BrokerStarter;
import com.alishangtian.mtimer.common.util.JSONUtils;
import com.alishangtian.mtimer.model.metrics.MtimerBrokerMetrics;
import com.alishangtian.mtimer.remoting.MtimerCommand;
import com.alishangtian.mtimer.remoting.processor.NettyRequestProcessor;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @Desc BrokerBrokerMetricsProcessor
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Slf4j
public class BrokerBrokerMetricsProcessor implements NettyRequestProcessor {
    private BrokerStarter brokerStarter;

    public BrokerBrokerMetricsProcessor(BrokerStarter brokerStarter) {
        this.brokerStarter = brokerStarter;
    }

    @Override
    public MtimerCommand processRequest(ChannelHandlerContext ctx, MtimerCommand request) throws Exception {
        MtimerBrokerMetrics selfMetrics = this.brokerStarter.getMetrics();
        return MtimerCommand.builder().result(1)
                .load(JSONUtils.toJSONString(selfMetrics).getBytes())
                .build();
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
