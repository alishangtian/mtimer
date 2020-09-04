package com.alishangtian.mtimer.broker.config;

import com.alishangtian.mtimer.common.redis.JedisPoolFactory;
import com.alishangtian.mtimer.core.config.ScannerConfig;
import com.alishangtian.mtimer.remoting.config.NettyClientConfig;
import com.alishangtian.mtimer.remoting.config.NettyServerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import redis.clients.jedis.JedisCluster;

/**
 * @Desc ServerConfiguration
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Configuration
public class ServerConfiguration {
    @ConfigurationProperties(prefix = "mtimer.server")
    @Bean
    public NettyServerConfig nettyServerConfig() {
        return new NettyServerConfig();
    }

    @ConfigurationProperties(prefix = "mtimer.client")
    @Bean
    public NettyClientConfig nettyClientConfig() {
        return new NettyClientConfig();
    }

    @ConfigurationProperties(prefix = "mtimer.broker")
    @Bean
    public BrokerConfig brokerConfig() {
        return new BrokerConfig();
    }

    @ConfigurationProperties(prefix = "mtimer.redis")
    @Bean
    public JedisClusterConfig jedisClusterConfig() {
        return new JedisClusterConfig();
    }

    @ConfigurationProperties(prefix = "mtimer.scanner")
    @Bean
    public ScannerConfig scannerConfig() {
        return new ScannerConfig();
    }

    @Bean
    @DependsOn("jedisClusterConfig")
    public JedisCluster jedisCluster() {
        return JedisPoolFactory.getJedisCluster(jedisClusterConfig().getNodes(), jedisClusterConfig().getTimeout());
    }

}
