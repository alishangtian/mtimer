package com.alishangtian.mtimer.processor;

import com.alishangtian.mtimer.broker.controller.BrokerStarter;
import com.alishangtian.mtimer.common.util.JSONUtils;
import com.alishangtian.mtimer.remoting.MtimerCommand;
import com.alishangtian.mtimer.remoting.common.MtimerHelper;
import com.alishangtian.mtimer.remoting.processor.NettyRequestProcessor;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @Desc ClientAskLeaderForBrokerTopologyProcessor
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Slf4j
public class ClientAskLeaderForBrokerTopologyProcessor implements NettyRequestProcessor {
    private BrokerStarter brokerStarter;

    public ClientAskLeaderForBrokerTopologyProcessor(BrokerStarter brokerStarter) {
        this.brokerStarter = brokerStarter;
    }

    @Override
    public MtimerCommand processRequest(ChannelHandlerContext ctx, MtimerCommand request) throws Exception {
        log.info("ClientAskLeaderForBrokerTopologyProcessor clientAddress {}", MtimerHelper.parseChannelRemoteAddr(ctx.channel()));
        return MtimerCommand.builder().load(JSONUtils.toJSONString(this.brokerStarter.getBrokerWrapperMap()).getBytes()).result(1).build();
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
