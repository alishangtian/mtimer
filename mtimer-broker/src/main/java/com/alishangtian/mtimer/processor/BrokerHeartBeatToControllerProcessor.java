package com.alishangtian.mtimer.processor;

import com.alishangtian.mtimer.broker.controller.BrokerStarter;
import com.alishangtian.mtimer.common.protocol.RequestCode;
import com.alishangtian.mtimer.common.util.JSONUtils;
import com.alishangtian.mtimer.model.core.BrokerWrapper;
import com.alishangtian.mtimer.remoting.MtimerCommand;
import com.alishangtian.mtimer.remoting.processor.NettyRequestProcessor;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @Desc BrokerHeartBeatToControllerProcessor
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Slf4j
public class BrokerHeartBeatToControllerProcessor implements NettyRequestProcessor {
    private BrokerStarter brokerStarter;

    public BrokerHeartBeatToControllerProcessor(BrokerStarter brokerStarter) {
        this.brokerStarter = brokerStarter;
    }

    @Override
    public MtimerCommand processRequest(ChannelHandlerContext ctx, MtimerCommand request) throws Exception {
        BrokerWrapper brokerWrapper = brokerStarter.heartBeat(request.getHostAddr());
        if (brokerStarter.getWaitingFollowerTopology().get() && null != request.getLoad() && request.getLoad().length > 0) {
            brokerStarter.checkFullTopology(request);
        }
        return MtimerCommand.builder().result(1)
                .load(JSONUtils.toJSONString(brokerStarter.getBrokerWrapperMap()).getBytes())
                .waitingFollowerTopology(brokerStarter.getWaitingFollowerTopology().get())
                .code(null == brokerWrapper ? RequestCode.LEADER_DOSE_NOT_HAS_YOUR_WRAPPER : -1)
                .build();
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
