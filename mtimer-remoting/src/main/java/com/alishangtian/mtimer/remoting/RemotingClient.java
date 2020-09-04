package com.alishangtian.mtimer.remoting;

import com.alishangtian.mtimer.remoting.callback.InvokeCallback;
import com.alishangtian.mtimer.remoting.exception.RemotingConnectException;
import com.alishangtian.mtimer.remoting.exception.RemotingSendRequestException;
import com.alishangtian.mtimer.remoting.exception.RemotingTimeoutException;
import com.alishangtian.mtimer.remoting.exception.RemotingTooMuchRequestException;
import com.alishangtian.mtimer.remoting.processor.NettyRequestProcessor;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @Desc RemotingClient
 * @Time 2020/08/30
 * @Author alishangtian
 */
public interface RemotingClient extends RemotingService {
    void updateNameServerAddressList(final List<String> addrs);

    List<String> getNameServerAddressList();

    MtimerCommand invokeSync(final String addr, final MtimerCommand request,
                             final long timeoutMillis) throws InterruptedException, RemotingConnectException,
            RemotingSendRequestException, RemotingTimeoutException;

    void invokeAsync(final String addr, final MtimerCommand request, final long timeoutMillis,
                     final InvokeCallback invokeCallback) throws InterruptedException, RemotingConnectException,
            RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;

    void invokeOneway(final String addr, final MtimerCommand request, final long timeoutMillis)
            throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException,
            RemotingTimeoutException, RemotingSendRequestException;

    void registerProcessor(final int requestCode, final NettyRequestProcessor processor,
                           final ExecutorService executor);

    void setCallbackExecutor(final ExecutorService callbackExecutor);

    ExecutorService getCallbackExecutor();

    boolean isChannelWritable(final String addr);
}
