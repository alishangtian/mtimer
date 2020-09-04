package com.alishangtian.mtimer.model.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Desc MtimerBrokerMetrics
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MtimerBrokerMetrics {
    private long brokerAddSuccessCount;
    private long brokerTriggerdSuccessCount;
    private long brokerTriggerdCount;
    private String brokerAddress;
    private String role;
    private String status;
}
