package com.alishangtian.mtimer.client;

import com.alishangtian.mtimer.model.metrics.MtimerMetrics;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * @Desc BrokerStatus
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrokerStatus {
    private MtimerMetrics metrics;
    private String clusterName;
    private ConcurrentMap<String, String> zsetBrokerMap;
    @lombok.Builder.Default
    private Map<String, Long> zsetMap = new HashMap<>();
    @lombok.Builder.Default
    private Map<String, Long> copyMap = new HashMap<>();
    @lombok.Builder.Default
    private Map<String, Long> retryMap = new HashMap<>();
    @lombok.Builder.Default
    private Map<String, Long> listMap = new HashMap<>();
}
