package com.alishangtian.mtimer.processor;

import com.alishangtian.mtimer.broker.controller.BrokerStarter;
import com.alishangtian.mtimer.common.util.JSONUtils;
import com.alishangtian.mtimer.model.core.BrokerWrapper;
import com.alishangtian.mtimer.remoting.MtimerCommand;
import com.alishangtian.mtimer.remoting.processor.NettyRequestProcessor;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @Desc NewLeaderAskBrokerForRebalanceProcessor
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Slf4j
public class NewLeaderAskBrokerForRebalanceProcessor implements NettyRequestProcessor {
    private BrokerStarter brokerStarter;

    public NewLeaderAskBrokerForRebalanceProcessor(BrokerStarter brokerStarter) {
        this.brokerStarter = brokerStarter;
    }

    /**
     * @param ctx
     * @param request
     * @return
     * @throws Exception
     */
    @Override
    public MtimerCommand processRequest(ChannelHandlerContext ctx, MtimerCommand request) throws Exception {
        log.info("NewLeaderAskBrokerForRebalanceProcessor brokerAddr {} borkerWrapper {}", request.getHostAddr(), JSONUtils.parseObject(request.getLoad(), BrokerWrapper.class));
        this.brokerStarter.leaderAskForRebalance(request);
        MtimerCommand mtimerCommand = MtimerCommand.builder().result(1).build();
        return mtimerCommand;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
