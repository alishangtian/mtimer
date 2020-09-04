package com.alishangtian.mtimer.service;

import com.alishangtian.mtimer.common.util.JSONUtils;

import com.alishangtian.mtimer.core.service.MtimerService;
import com.alishangtian.mtimer.model.core.MtimerRequest;
import com.alishangtian.mtimer.model.core.MtimerResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisCluster;

/**
 * @Desc MtimerServiceImpl
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Service("mtimerService")
@Slf4j
public class MtimerServiceImpl implements MtimerService {
    @Autowired
    JedisCluster jedisCluster;

    @Override
    public MtimerResult addMtimer(MtimerRequest mtimerRequest) {
        log.debug("mtimerService add mtimer,partition:{}", mtimerRequest.getPartition());
        long result = jedisCluster.zadd(mtimerRequest.getPartition(),
                mtimerRequest.getCallBackTime(), JSONUtils.toJSONString(mtimerRequest));
        if(result == 0){
            return MtimerResult.builder().msg("add same member").success(false).build();
        }else {
            return MtimerResult.builder().success(true).build();
        }
    }

    @Override
    public Long deleteMtimer(MtimerRequest mtimerRequest) {
        //todo
        return 1L;
    }
}
