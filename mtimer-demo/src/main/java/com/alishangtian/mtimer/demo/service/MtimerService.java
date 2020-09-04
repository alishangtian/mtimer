package com.alishangtian.mtimer.demo.service;

import com.alishangtian.mtimer.client.BrokerStatus;
import com.alishangtian.mtimer.client.DefaultMtimerClient;
import com.alishangtian.mtimer.model.core.MtimerRequest;
import com.alishangtian.mtimer.model.core.MtimerResult;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @Desc MtimerService
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Service
@Log4j2
public class MtimerService {
    @Autowired(required = false)
    @Qualifier("mtimerClient")
    private DefaultMtimerClient mtimerClient;

    /**
     * 添加定时任务
     *
     * @param mtimerRequest
     * @return
     */
    public MtimerResult addMtimer(MtimerRequest mtimerRequest) {
        return mtimerClient.insertMtimer(mtimerRequest);
    }
    /**
     * @Description getBrokerStatus
     * @Date 2020/8/5 上午11:28
     * @Author maoxiaobing
     **/
    public BrokerStatus getBrokerStatus() {
        return mtimerClient.brokerStatus();
    }

    /**
     * @Description clearRedisData
     * @Date 2020/8/5 下午4:38
     * @Author maoxiaobing
     **/
    public void clearRedisData() {
        mtimerClient.clearRedisData();
    }
}
