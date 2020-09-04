package com.alishangtian.mtimer.common.util;

/**
 * @Desc MtimerUtils
 * @Time 2020/08/30
 * @Author alishangtian
 */
public class MtimerUtils {
    private static final String CONS_CLSUTER_PARTITION_PATTERN = "%s:%s";
    private static final String CONS_CLSUTER_METRICS_PATTERN = "metrics:%s";
    private static final String CONS_LEADER_KEY_PATTERN = "LEADER:%s";
    private static final String CONS_PARTITION_KEY_PATTERN = "zset:{%s}";
    private static final String CONS_XITMER_CLIENT_MAP_KEY = "%s:%s";
    private static final String CONS_CLUSTER_MTIMER_BROKER_ADD_SUCCESS_COUNT_KEY = "broker:add:success:list:%s";
    private static final String CONS_CLUSTER_MTIMER_TRIGGERD_CLIENT_COUNT_KEY = "client:triggered:list:%s";
    private static final String CONS_CLUSTER_MTIMER_TRIGGERD_BROKER_COUNT_KEY = "broker:triggered:list:%s";
    private static final String CONS_CLUSTER_MTIMER_TRIGGERD_SUCCESS_BROKER_COUNT_KEY = "broker:triggered:success:list:%s";

    /**
     * @Description 构建集群分片key
     * @Date 2020/7/20 下午4:06
     * @Author maoxiaobing
     **/
    public static String constructClusterPartitions(String partitionPrefix, String clusterName) {
        return String.format(CONS_CLSUTER_PARTITION_PATTERN, partitionPrefix, clusterName);
    }

    /**
     * @Description 构建集群leader key
     * @Date 2020/7/20 下午4:11
     * @Author maoxiaobing
     **/
    public static String constructLeaderKey(String clusterName) {
        return String.format(CONS_LEADER_KEY_PATTERN, clusterName);
    }

    /**
     * @Description 构建集群分片key
     * @Date 2020/7/21 下午2:32
     * @Author maoxiaobing
     **/
    public static String constructPartitionPrefix(String partition) {
        return String.format(CONS_PARTITION_KEY_PATTERN, partition);
    }

    /**
     * @Description 构建broker端 client map key
     * @Date 2020/7/21 下午5:46
     * @Author maoxiaobing
     **/
    public static String constructChannelMapKey(String groupKey, String appKey) {
        return String.format(CONS_XITMER_CLIENT_MAP_KEY, groupKey, appKey);
    }

    /**
     * @Description
     * @Date 2020/8/6 上午10:14
     * @Author maoxiaobing
     **/
    public static String constructBrokerAddSuccessCountKey(String clusterName) {
        return String.format(CONS_CLUSTER_MTIMER_BROKER_ADD_SUCCESS_COUNT_KEY, clusterName);
    }

    /**
     * @Description
     * @Date 2020/8/6 上午10:14
     * @Author maoxiaobing
     **/
    public static String constructClientTriggeredCountKey(String clusterName) {
        return String.format(CONS_CLUSTER_MTIMER_TRIGGERD_CLIENT_COUNT_KEY, clusterName);
    }

    /**
     * @Description
     * @Date 2020/8/6 下午2:28
     * @Author maoxiaobing
     **/
    public static String constructBrokerTriggeredCount(String clusterName) {
        return String.format(CONS_CLUSTER_MTIMER_TRIGGERD_BROKER_COUNT_KEY, clusterName);
    }

    /**
     * @Description
     * @Date 2020/8/6 下午2:28
     * @Author maoxiaobing
     **/
    public static String constructBrokerTriggeredSuccessCount(String clusterName) {
        return String.format(CONS_CLUSTER_MTIMER_TRIGGERD_SUCCESS_BROKER_COUNT_KEY, clusterName);
    }

    /**
     * @Description
     * @Date 2020/8/6 下午2:28
     * @Author maoxiaobing
     **/
    public static String constructCLusterMetricsKey(String clusterName) {
        return String.format(CONS_CLSUTER_METRICS_PATTERN, clusterName);
    }
}
