package com.alishangtian.mtimer.remoting.callback;

import com.alishangtian.mtimer.remoting.common.ResponseFuture;

/**
 * @Desc InvokeCallback
 * @Time 2020/08/30
 * @Author alishangtian
 */
public interface InvokeCallback {
    void operationComplete(final ResponseFuture responseFuture);
}
