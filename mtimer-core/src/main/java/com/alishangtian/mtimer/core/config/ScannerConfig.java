package com.alishangtian.mtimer.core.config;

import lombok.Data;

/**
 * @Desc ScannerConfig
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Data
public class ScannerConfig {
    private int scanZsetThreadPoolCoreSize;
    private int scanZsetThreadPoolMaxSize;
    private int scanZsetThreadPoolQueueMaxSize;

    private int scanListThreadPoolCoreSize;
    private int scanListThreadPoolMaxSize;
    private int scanListThreadPoolQueueMaxSize;

    private int triggerThreadPoolCoreSize;
    private int triggerThreadPoolMaxSize;
    private int triggerThreadPoolQueueMaxSize;
}
