package com.alishangtian.mtimer.processor;

import com.alishangtian.mtimer.broker.controller.BrokerStarter;
import com.alishangtian.mtimer.common.RemotingCommandResultEnums;
import com.alishangtian.mtimer.common.util.JSONUtils;
import com.alishangtian.mtimer.exception.AddMtimerException;
import com.alishangtian.mtimer.model.core.MtimerRequest;
import com.alishangtian.mtimer.model.core.MtimerResult;
import com.alishangtian.mtimer.remoting.MtimerCommand;
import com.alishangtian.mtimer.remoting.processor.NettyRequestProcessor;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.log4j.Log4j2;

/**
 * @Desc AddMtimerProcessor
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Log4j2
public class AddMtimerProcessor implements NettyRequestProcessor {
    private BrokerStarter brokerStarter;

    public AddMtimerProcessor(BrokerStarter brokerStarter) {
        this.brokerStarter = brokerStarter;
    }

    @Override
    public MtimerCommand processRequest(ChannelHandlerContext ctx, MtimerCommand request) throws Exception {
        return this.process(request);
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }

    private MtimerCommand process(MtimerCommand request) {
        MtimerCommand response = MtimerCommand.builder().build();
        log.debug("add mtimer mtimerRequest:{}", JSONUtils.parseObject(request.getLoad(), MtimerRequest.class));
        try {
            MtimerResult result = this.brokerStarter.addMtimer(request);
            if (result.isSuccess()) {
                this.brokerStarter.brokerAddTimerSuccessCounter.incrementAndGet();
            }
            log.debug("add mtimer result:{}", JSONUtils.toJSONString(result));
            response.setResult(result.isSuccess() ? 1 : 0);
            response.setRemark(result.getMsg());
            return response;
        } catch (AddMtimerException e) {
            log.error("add mtimer error:{}", e.getMessage(), e);
            response.setResult(RemotingCommandResultEnums.FAILED.getResult());
            response.setRemark(e.getMessage());
        }
        return response;
    }
}
