package com.alishangtian.mtimer.client.event;

import com.alishangtian.mtimer.remoting.ChannelEventListener;
import com.alishangtian.mtimer.remoting.common.MtimerUtil;
import com.google.common.collect.ArrayListMultimap;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * @Desc DefaultChannelEventListener
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Slf4j
public class DefaultChannelEventListener implements ChannelEventListener {

    ArrayListMultimap<String, Channel> channelMap = ArrayListMultimap.create();

    private ConcurrentHashMap<String, CountDownLatch> countDownLatchMap = new ConcurrentHashMap<>();

    public void addCountdownLatch(String hostAddr, CountDownLatch countDownLatch) {
        countDownLatchMap.put(hostAddr, countDownLatch);
    }

    @Override
    public void onChannelConnect(String remoteAddr, Channel channel) {
        log.info("channel active: {} >> {}", remoteAddr, MtimerUtil.getLocalAddress());
        channelMap.put(remoteAddr, channel);
        CountDownLatch countDownLatch;
        if (null != (countDownLatch = countDownLatchMap.get(remoteAddr))) {
            countDownLatch.countDown();
        }
    }

    @Override
    public void onChannelClose(String remoteAddr, Channel channel) {
        log.info("channel inactive: {} >> {}", remoteAddr, MtimerUtil.getLocalAddress());
        channelMap.remove(remoteAddr, channel);
    }

    @Override
    public void onChannelException(String remoteAddr, Channel channel) {
        log.info("channel exception: {} >> {}", remoteAddr, MtimerUtil.getLocalAddress());
        channelMap.remove(remoteAddr, channel);
    }

    @Override
    public void onChannelIdle(String remoteAddr, Channel channel) {
        log.info("channel idle: {} >> {}", remoteAddr, MtimerUtil.getLocalAddress());
    }

    @Override
    public Channel getChannel(String addr) {
        List<Channel> channelList = this.channelMap.get(addr);
        return channelList.size() > 0 ? channelList.get(0) : null;
    }

    public Channel getChannel() {
        if (!this.channelMap.values().isEmpty()) {
            this.channelMap.values().iterator().next();
        }
        return null;
    }

}
