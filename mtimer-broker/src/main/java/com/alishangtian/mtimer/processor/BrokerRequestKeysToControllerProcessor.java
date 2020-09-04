package com.alishangtian.mtimer.processor;

import com.alishangtian.mtimer.broker.controller.BrokerStarter;
import com.alishangtian.mtimer.common.util.JSONUtils;
import com.alishangtian.mtimer.remoting.MtimerCommand;
import com.alishangtian.mtimer.remoting.processor.NettyRequestProcessor;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @Desc BrokerRequestKeysToControllerProcessor
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Slf4j
public class BrokerRequestKeysToControllerProcessor implements NettyRequestProcessor {
    private BrokerStarter brokerStarter;

    public BrokerRequestKeysToControllerProcessor(BrokerStarter brokerStarter) {
        this.brokerStarter = brokerStarter;
    }

    /**
     * TODO
     *
     * @param ctx
     * @param request
     * @return
     * @throws Exception
     */
    @Override
    public MtimerCommand processRequest(ChannelHandlerContext ctx, MtimerCommand request) throws Exception {
        log.info("BrokerRequestKeysToController remoteServerHost:[{}]", request.getHostAddr());
        List<String> keys = this.brokerStarter.requestZsetKeys(ctx.channel(), request.getHostAddr());
        log.info("BrokerRequestKeysToController keys:{}", keys);
        return MtimerCommand.builder().load(JSONUtils.toJSONString(keys).getBytes())
                .waitingFollowerTopology(brokerStarter.getWaitingFollowerTopology().get())
                .result(1)
                .build();
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
