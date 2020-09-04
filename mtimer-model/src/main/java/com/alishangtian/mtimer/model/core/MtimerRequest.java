package com.alishangtian.mtimer.model.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Desc MtimerRequest
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MtimerRequest {
    /**
     * 请求码
     */
    private int code;
    /**
     * 组key
     */
    private String groupKey;
    /**
     * 应用key
     */
    private String appKey;
    /**
     * 回调内容
     */
    private String callBackBody;
    /**
     * 回调时间
     */
    private Long callBackTime;
    /**
     * 集群名称
     */
    private String clusterName;

    /**
     * 分片名称
     */
    private String partition;

}
