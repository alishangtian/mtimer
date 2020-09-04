package com.alishangtian.mtimer.client.processor;

import com.alishangtian.mtimer.model.core.MtimerRequest;
import redis.clients.jedis.JedisCluster;

/**
 * @Desc MtimerProcessor
 * @Time 2020/08/30
 * @Author alishangtian
 */
public interface MtimerProcessor {
    boolean process(MtimerRequest mtimerRequest);

    void setJedisCluster(JedisCluster jedisCluster);

    void setClusterName(String clusterName);
}
