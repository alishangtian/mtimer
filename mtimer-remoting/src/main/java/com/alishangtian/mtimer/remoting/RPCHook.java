package com.alishangtian.mtimer.remoting;

/**
 * @Desc RPCHook
 * @Time 2020/08/30
 * @Author alishangtian
 */
public interface RPCHook {
    void doBeforeRequest(final String remoteAddr, final MtimerCommand request);

    void doAfterResponse(final String remoteAddr, final MtimerCommand request,
                         final MtimerCommand response);
}
