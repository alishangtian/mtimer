package com.alishangtian.mtimer.remoting;

import io.netty.channel.Channel;

/**
 * @Desc ChannelEventListener
 * @Time 2020/08/30
 * @Author alishangtian
 */
public interface ChannelEventListener {
    void onChannelConnect(final String remoteAddr, final Channel channel);

    void onChannelClose(final String remoteAddr, final Channel channel);

    void onChannelException(final String remoteAddr, final Channel channel);

    void onChannelIdle(final String remoteAddr, final Channel channel);

    Channel getChannel(String address);

}
