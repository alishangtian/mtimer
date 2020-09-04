package com.alishangtian.mtimer.broker.config;

import lombok.Data;

/**
 * @Desc JedisClusterConfig
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Data
public class JedisClusterConfig {
    private String nodes;
    private int timeout;
}
