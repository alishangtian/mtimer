package com.alishangtian.mtimer.common.redis;

import redis.clients.jedis.*;

import java.util.HashSet;
import java.util.Set;

/**
 * @Desc JedisPoolFactory
 * @Time 2020/08/30
 * @Author alishangtian
 */
public class JedisPoolFactory {
    /**
     * 最小空闲数
     */
    private static final int DEFAULT_MIN_IDLE = 8;
    /**
     * 最大空闲数
     */
    private static final int DEFAULT_MAX_IDLE = 20;
    /**
     * 最大连接数
     */
    private static final int DEFAULT_MAX_TOTAL = 100;

    /**
     * 命令执行默认超时时间
     */
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    /**
     * 获取单主模式下的jedispool
     *
     * @param host
     * @param port
     * @param timeout
     * @param minIdle
     * @param maxIdle
     * @param maxTotal
     * @return
     */
    public static JedisPool getMasterSlaveJedisPool(String host, int port, int timeout, int minIdle, int maxIdle, int maxTotal) {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(maxTotal);
        jedisPoolConfig.setMaxIdle(maxIdle);
        jedisPoolConfig.setMinIdle(minIdle);
        jedisPoolConfig.setBlockWhenExhausted(false);
        jedisPoolConfig.setNumTestsPerEvictionRun(4);
        return new JedisPool(jedisPoolConfig, host, port, timeout);
    }

    /**
     * 获取单主模式下的jedispool
     *
     * @param host
     * @param port
     * @param timeout
     * @return
     */
    public static JedisPool getMasterSlaveJedisPool(String host, int port, int timeout) {
        return getMasterSlaveJedisPool(host, port, timeout, DEFAULT_MIN_IDLE, DEFAULT_MAX_IDLE, DEFAULT_MAX_TOTAL);
    }

    /**
     * 获取单主模式下的jedispool
     *
     * @param host
     * @param port
     * @return
     */
    public static JedisPool getMasterSlaveJedisPool(String host, int port) {
        return getMasterSlaveJedisPool(host, port, DEFAULT_TIMEOUT_MS);
    }

    /**
     * 获取集群模式下的jedisCluster
     *
     * @param hosts    "127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381"
     * @param timeout
     * @param minIdle
     * @param maxIdle
     * @param maxTotal
     * @return
     */
    public static JedisCluster getJedisCluster(String hosts, int timeout, int minIdle, int maxIdle, int maxTotal) {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(maxTotal);
        jedisPoolConfig.setMaxIdle(maxIdle);
        jedisPoolConfig.setMinIdle(minIdle);
        jedisPoolConfig.setBlockWhenExhausted(false);
        jedisPoolConfig.setNumTestsPerEvictionRun(4);
        String[] serverArray = hosts.split(",");
        Set<HostAndPort> nodes = new HashSet<>();
        for (String ipPort : serverArray) {
            String[] ipPortPair = ipPort.split(":");
            nodes.add(new HostAndPort(ipPortPair[0].trim(), Integer.valueOf(ipPortPair[1].trim())));
        }
        return new JedisCluster(nodes, timeout, jedisPoolConfig);
    }

    /**
     * 获取集群模式下的jedisCluster
     *
     * @param hosts
     * @param timeout
     * @return
     */
    public static JedisCluster getJedisCluster(String hosts, int timeout) {
        return getJedisCluster(hosts, timeout, DEFAULT_MIN_IDLE, DEFAULT_MAX_IDLE, DEFAULT_MAX_TOTAL);
    }

    /**
     * 获取集群模式下的jedisCluster
     *
     * @param hosts
     * @return
     */
    public static JedisCluster getJedisCluster(String hosts) {
        return getJedisCluster(hosts, DEFAULT_TIMEOUT_MS);
    }

    /**
     * 获取哨兵模式下的连接池
     *
     * @param masterName "master1"
     * @param sentinels  new HashSet<>(Arrays.asList(new String[]{"127.0.0.1:8001", "127.0.0.1:8002", "127.0.0.1:8003"}))
     * @param timeout
     * @param minIdle
     * @param maxIdle
     * @param maxTotal
     * @return
     */
    public static JedisSentinelPool getJedisSentinelPool(String masterName, Set<String> sentinels, int timeout, int minIdle, int maxIdle, int maxTotal) {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(maxTotal);
        jedisPoolConfig.setMaxIdle(maxIdle);
        jedisPoolConfig.setMinIdle(minIdle);
        jedisPoolConfig.setBlockWhenExhausted(false);
        jedisPoolConfig.setNumTestsPerEvictionRun(4);
        return new JedisSentinelPool(masterName, sentinels, jedisPoolConfig, timeout);
    }

    /**
     * 获取哨兵模式下的连接池
     *
     * @param masterName
     * @param sentinels
     * @param timeout
     * @return
     */
    public static JedisSentinelPool getJedisSentinelPool(String masterName, Set<String> sentinels, int timeout) {
        return getJedisSentinelPool(masterName, sentinels, timeout, DEFAULT_MIN_IDLE, DEFAULT_MAX_IDLE, DEFAULT_MAX_TOTAL);
    }

    /**
     * 获取哨兵模式下的连接池
     *
     * @param masterName
     * @param sentinels
     * @return
     */
    public static JedisSentinelPool getJedisSentinelPool(String masterName, Set<String> sentinels) {
        return getJedisSentinelPool(masterName, sentinels, DEFAULT_TIMEOUT_MS);
    }
}
