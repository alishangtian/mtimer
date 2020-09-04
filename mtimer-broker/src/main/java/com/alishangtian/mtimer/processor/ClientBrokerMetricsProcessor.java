package com.alishangtian.mtimer.processor;

import com.alishangtian.mtimer.broker.controller.BrokerStarter;
import com.alishangtian.mtimer.common.util.JSONUtils;
import com.alishangtian.mtimer.model.metrics.MtimerBrokerMetrics;
import com.alishangtian.mtimer.model.metrics.MtimerMetrics;
import com.alishangtian.mtimer.remoting.MtimerCommand;
import com.alishangtian.mtimer.remoting.processor.NettyRequestProcessor;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * @Desc ClientBrokerMetricsProcessor
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Slf4j
public class ClientBrokerMetricsProcessor implements NettyRequestProcessor {
    private BrokerStarter brokerStarter;

    public ClientBrokerMetricsProcessor(BrokerStarter brokerStarter) {
        this.brokerStarter = brokerStarter;
    }

    @Override
    public MtimerCommand processRequest(ChannelHandlerContext ctx, MtimerCommand request) throws Exception {
        MtimerBrokerMetrics selfMetrics = this.brokerStarter.getMetrics();
        MtimerMetrics mtimerMetrics = MtimerMetrics.builder()
                .clusterAddSuccessCount(selfMetrics.getBrokerAddSuccessCount())
                .clusterTriggerdCount(selfMetrics.getBrokerTriggerdCount())
                .clusterTriggerdSuccessCount(selfMetrics.getBrokerTriggerdSuccessCount())
                .clusterName(this.brokerStarter.getBrokerConfig().getClusterName())
                .build();
        mtimerMetrics.getMtimerMetrics().put(selfMetrics.getBrokerAddress(), selfMetrics);
        Set<String> keySet = this.brokerStarter.getBrokerWrapperMap().keySet();
        for (String s : keySet) {
            if (!s.equals(this.brokerStarter.getHostAddr())) {
                MtimerBrokerMetrics brokerMetrics = this.brokerStarter.getMetricsFromBroker(s);
                mtimerMetrics.getMtimerMetrics().put(s, brokerMetrics);
                mtimerMetrics.setClusterAddSuccessCount(mtimerMetrics.getClusterAddSuccessCount() + brokerMetrics.getBrokerAddSuccessCount());
                mtimerMetrics.setClusterTriggerdCount(mtimerMetrics.getClusterTriggerdCount() + brokerMetrics.getBrokerTriggerdCount());
                mtimerMetrics.setClusterTriggerdSuccessCount(mtimerMetrics.getClusterTriggerdSuccessCount() + brokerMetrics.getBrokerTriggerdSuccessCount());
            }
        }
        mtimerMetrics.setActiveClient(this.brokerStarter.getActiveClient());
        return MtimerCommand.builder().result(1).load(JSONUtils.toJSONString(mtimerMetrics).getBytes()).build();
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
