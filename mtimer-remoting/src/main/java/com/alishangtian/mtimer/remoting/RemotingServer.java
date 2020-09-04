package com.alishangtian.mtimer.remoting;

import com.alishangtian.mtimer.remoting.callback.InvokeCallback;
import com.alishangtian.mtimer.remoting.common.Pair;
import com.alishangtian.mtimer.remoting.exception.RemotingSendRequestException;
import com.alishangtian.mtimer.remoting.exception.RemotingTimeoutException;
import com.alishangtian.mtimer.remoting.exception.RemotingTooMuchRequestException;
import com.alishangtian.mtimer.remoting.processor.NettyRequestProcessor;
import io.netty.channel.Channel;

import java.util.concurrent.ExecutorService;

/**
 * @Desc RemotingServer
 * @Time 2020/08/30
 * @Author alishangtian
 */
public interface RemotingServer extends RemotingService {
    void registerProcessor(final int requestCode, final NettyRequestProcessor processor,
                           final ExecutorService executor);

    void registerDefaultProcessor(final NettyRequestProcessor processor, final ExecutorService executor);

    int localListenPort();

    Pair<NettyRequestProcessor, ExecutorService> getProcessorPair(final int requestCode);

    MtimerCommand invokeSync(final Channel channel, final MtimerCommand request,
                             final long timeoutMillis) throws InterruptedException, RemotingSendRequestException,
            RemotingTimeoutException;

    void invokeAsync(final Channel channel, final MtimerCommand request, final long timeoutMillis,
                     final InvokeCallback invokeCallback) throws InterruptedException,
            RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;

    void invokeOneway(final Channel channel, final MtimerCommand request, final long timeoutMillis)
            throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException,
            RemotingSendRequestException;
}
