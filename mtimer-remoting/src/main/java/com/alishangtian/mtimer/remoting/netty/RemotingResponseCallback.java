package com.alishangtian.mtimer.remoting.netty;

import com.alishangtian.mtimer.remoting.MtimerCommand;
/**
 * @Desc RemotingResponseCallback
 * @Time 2020/08/30
 * @Author alishangtian
 */
public interface RemotingResponseCallback {
    void callback(MtimerCommand response);
}
