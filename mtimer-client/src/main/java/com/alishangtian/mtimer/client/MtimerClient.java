package com.alishangtian.mtimer.client;

import com.alishangtian.mtimer.client.exception.InsertFailedException;
import com.alishangtian.mtimer.model.core.MtimerRequest;
import com.alishangtian.mtimer.model.core.MtimerResult;
import com.alishangtian.mtimer.remoting.MtimerCommand;

/**
 * @Desc MtimerClient
 * @Time 2020/08/30
 * @Author alishangtian
 */
public interface MtimerClient {
    /**
     * 插入增量定时任务
     *
     * @param mtimerRequest
     * @return
     */
    MtimerResult insertMtimer(MtimerRequest mtimerRequest) throws InsertFailedException;

    /**
     * 开启客户端
     *
     * @return
     */
    void start() throws InterruptedException;

    /**
     * @Description TODO
     * @Date 2020/8/5 下午12:37
     * @Author maoxiaobing
     **/
    BrokerStatus brokerStatus();

    /**
     * @Description TODO
     * @Date 2020/8/5 下午4:34
     * @Author maoxiaobing
     **/
    void clearRedisData();

    /**
     * 定时任务回调
     *
     * @param mtimerCommand
     * @return
     */
    boolean callBack(MtimerCommand mtimerCommand);
}
