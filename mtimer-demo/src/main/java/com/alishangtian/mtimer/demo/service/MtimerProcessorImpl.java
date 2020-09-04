package com.alishangtian.mtimer.demo.service;

import com.alishangtian.mtimer.client.processor.MtimerProcessor;
import com.alishangtian.mtimer.model.core.MtimerRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisCluster;

/**
 * @Desc MtimerProcessorImpl
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Service("mtimerProcessor")
@Slf4j
public class MtimerProcessorImpl implements MtimerProcessor {
    private JedisCluster jedisCluster;
    private String clusterName;

    @Override
    public boolean process(MtimerRequest mtimerRequest) {
        long delay = System.currentTimeMillis() - mtimerRequest.getCallBackTime();
        if (delay > 200L) {
            log.warn("timer triggered delay {}:ms", delay);
        }
        return true;
    }

    @Override
    public void setJedisCluster(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    @Override
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }
}
