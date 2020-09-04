package com.alishangtian.mtimer.model.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Desc MtimerMetrics
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MtimerMetrics {
    @lombok.Builder.Default
    Map<String, MtimerBrokerMetrics> mtimerMetrics = new HashMap<>();
    Map<String, List<String>> activeClient;
    private long clusterAddSuccessCount;
    private long clusterTriggerdSuccessCount;
    private long clusterTriggerdCount;
    private long copyTotalCount;
    private long retryTotalCount;
    private String clusterName;

}
