package com.alishangtian.mtimer.processor;

import com.alishangtian.mtimer.broker.controller.BrokerStarter;
import com.alishangtian.mtimer.common.util.JSONUtils;
import com.alishangtian.mtimer.common.util.MtimerUtils;
import com.alishangtian.mtimer.core.processor.InvokerMtimerProcessor;
import com.alishangtian.mtimer.model.core.MtimerRequest;
import com.alishangtian.mtimer.remoting.ChannelEventListener;
import com.alishangtian.mtimer.remoting.MtimerCommand;
import com.alishangtian.mtimer.remoting.common.MtimerHelper;
import com.alishangtian.mtimer.remoting.processor.NettyRequestProcessor;
import com.google.common.collect.HashMultimap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.log4j.Log4j2;

import java.util.*;

/**
 * @Desc ServerChannelProcessor
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Log4j2
public class ServerChannelProcessor implements ChannelEventListener, NettyRequestProcessor, InvokerMtimerProcessor {
    private HashMultimap<String, Channel> activeChannel = HashMultimap.create();
    private BrokerStarter brokerStarter;

    public ServerChannelProcessor(BrokerStarter brokerStarter) {
        this.brokerStarter = brokerStarter;
    }

    @Override
    public void onChannelConnect(String remoteAddr, Channel channel) {
        log.info("channel connected address:{}", remoteAddr);
    }

    @Override
    public void onChannelClose(String remoteAddr, Channel channel) {
        log.info("channel closed address:{}", remoteAddr);
        activeChannel.values().remove(channel);
    }

    @Override
    public void onChannelException(String remoteAddr, Channel channel) {
        log.info("channel exception address:{}", remoteAddr);
        activeChannel.values().remove(channel);
    }

    @Override
    public void onChannelIdle(String remoteAddr, Channel channel) {
    }

    @Deprecated
    @Override
    public Channel getChannel(String address) {
        return null;
    }

    /**
     * add load balance
     *
     * @Author maoxiaobing
     * @Description getChannel
     * @Date 2020/6/16
     * @Param [groupKey, appKey]
     * @Return io.netty.channel.Channel
     */
    public Set<Channel> getChannel(String groupKey, String appKey) {
        return this.activeChannel.get(MtimerUtils.constructChannelMapKey(groupKey, appKey));
    }

    @Override
    public MtimerCommand processRequest(ChannelHandlerContext ctx, MtimerCommand request) {
        MtimerRequest mtimerRequest = JSONUtils.parseObject(request.getLoad(), MtimerRequest.class);
        activeChannel.put(MtimerUtils.constructChannelMapKey(mtimerRequest.getGroupKey(), mtimerRequest.getAppKey()), ctx.channel());
        log.info("client {} register success groupKey:[{}] appKey:[{}]", MtimerHelper.parseChannelRemoteAddr(ctx.channel()), mtimerRequest.getGroupKey(), mtimerRequest.getAppKey());
        MtimerCommand response = MtimerCommand.builder().result(1).build();
        if (this.brokerStarter.isLeader()) {
            response.setLoad(JSONUtils.toJSONString(this.brokerStarter.getBrokerWrapperMap()).getBytes());
        }
        return response;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }

    /**
     * invokeMtimerTrigger
     *
     * @Description
     * @Date 2020/6/23 下午5:35
     * @Author maoxiaobing
     **/
    @Override
    public boolean invokeMtimerTrigger(MtimerRequest mtimerRequest) {
        Set<Channel> channels = getChannel(mtimerRequest.getGroupKey(), mtimerRequest.getAppKey());
        if (channels.isEmpty()) {
            return false;
        }
        List<Channel> channelList = new ArrayList<>(channels);
        Collections.shuffle(channelList);
        Iterator<Channel> channelIterator = channelList.iterator();
        while (channelIterator.hasNext()) {
            Channel channel = channelIterator.next();
            if (null == channel || !channel.isActive()) {
                continue;
            }
            return this.brokerStarter.triggerMtimer(channel, mtimerRequest);
        }
        return false;
    }

    /**
     * @Description getActiveClient
     * @Date 2020/8/18 上午10:19
     * @Author maoxiaobing
     **/
    public Map<String, List<String>> getActiveClient() {
        Map<String, List<String>> clients = new HashMap<>();
        this.activeChannel.asMap().forEach((s, channels) -> {
            List<String> channelInfo = new ArrayList<>(channels.size());
            channels.forEach(channel -> {
                channelInfo.add(MtimerHelper.parseChannelRemoteAddr(channel));
            });
            clients.put(s, channelInfo);
        });
        return clients;
    }
}
