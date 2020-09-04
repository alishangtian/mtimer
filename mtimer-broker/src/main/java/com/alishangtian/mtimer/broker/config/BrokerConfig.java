package com.alishangtian.mtimer.broker.config;

import lombok.Data;

/**
 * @Desc BrokerConfig
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Data
public class BrokerConfig {
    private String clusterName;
    private boolean startScanner;
    private String host;
    private int leaderFailThreshold;
    private int joinClusterFailThreshold;
    private long timeoutThreshold;
    private long heartbeatInterval;
    private long keepLeadingInterval;
    private long checkFollowerInterval;
    private int partitionCount;
}
