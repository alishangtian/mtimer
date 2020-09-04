package com.alishangtian.mtimer.client.processor;

import com.alishangtian.mtimer.common.util.JSONUtils;
import com.alishangtian.mtimer.common.util.MtimerUtils;
import com.alishangtian.mtimer.model.core.MtimerRequest;
import com.alishangtian.mtimer.remoting.MtimerCommand;
import com.alishangtian.mtimer.remoting.processor.NettyRequestProcessor;
import io.netty.channel.ChannelHandlerContext;
import lombok.Builder;
import redis.clients.jedis.JedisCluster;

/**
 * @Desc InvokeMtimerProcessor
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Builder
public class InvokeMtimerProcessor implements NettyRequestProcessor {

    private MtimerProcessor mtimerProcessor;
    private JedisCluster jedisCluster;
    private String clusterName;

    @Override
    public MtimerCommand processRequest(ChannelHandlerContext ctx, MtimerCommand request) {
        jedisCluster.lpush(MtimerUtils.constructClientTriggeredCountKey(clusterName), "1");
        boolean result = mtimerProcessor.process(JSONUtils.parseObject(request.getLoad(), MtimerRequest.class));
        MtimerCommand response = MtimerCommand.builder().result(result ? 1 : 0).build();
        return response;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
