package com.alishangtian.mtimer.client;

import com.alishangtian.mtimer.remoting.MtimerCommand;

/**
 * @Desc MtimerCallBack
 * @Time 2020/08/30
 * @Author alishangtian
 */
public interface MtimerCallBack {
    public int callBack(MtimerCommand mtimerCommand);
}
