package com.alishangtian.mtimer.configuration;

import com.alishangtian.mtimer.client.DefaultMtimerClient;
import com.alishangtian.mtimer.client.config.ClientConfig;
import com.alishangtian.mtimer.client.event.DefaultChannelEventListener;
import com.alishangtian.mtimer.client.processor.MtimerProcessor;
import com.alishangtian.mtimer.common.redis.JedisPoolFactory;
import com.alishangtian.mtimer.remoting.config.NettyClientConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisCluster;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Desc MtimerClientAutoConfiguration
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Configuration
@ConditionalOnProperty(name = "mtimer.config.clientEnabled", havingValue = "true")
public class MtimerClientAutoConfiguration {
    @Autowired
    MtimerProcessor mtimerProcessor;

    @Bean
    @ConditionalOnMissingBean(NettyClientConfig.class)
    @ConfigurationProperties(prefix = "mtimer.client")
    public NettyClientConfig nettyClientConfig() {
        return new NettyClientConfig();
    }

    @Bean
    @ConditionalOnMissingBean(ClientConfig.class)
    @ConfigurationProperties(prefix = "mtimer.config")
    public ClientConfig clientConfig() {
        return new ClientConfig();
    }

    @Bean("mtimerClient")
    public DefaultMtimerClient mtimerClient(NettyClientConfig nettyClientConfig, ClientConfig clientConfig) {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(4, new ThreadFactory() {
            AtomicInteger nums = new AtomicInteger();

            @Override
            public Thread newThread(Runnable runnable) {
                return new Thread(runnable, "mtimer-client-scheduled-pool-thread-" + nums.getAndIncrement());
            }
        });
        JedisCluster jedisCluster = JedisPoolFactory.getJedisCluster(clientConfig.getRegisters(), clientConfig.getTimeout());
        mtimerProcessor.setJedisCluster(jedisCluster);
        DefaultMtimerClient client = DefaultMtimerClient.builder()
                .config(nettyClientConfig)
                .mtimerProcessor(mtimerProcessor)
                .defaultChannelEventListener(new DefaultChannelEventListener())
                .clientConfig(clientConfig)
                .scheduledThreadPoolExecutor(scheduledThreadPoolExecutor)
                .jedisCluster(jedisCluster)
                .publicExecutor(null)
                .build();
        client.start();
        return client;
    }
}
