package com.alishangtian.mtimer.client;

import com.alishangtian.mtimer.client.config.ClientConfig;
import com.alishangtian.mtimer.client.event.DefaultChannelEventListener;
import com.alishangtian.mtimer.client.processor.InvokeMtimerProcessor;
import com.alishangtian.mtimer.client.processor.MtimerProcessor;
import com.alishangtian.mtimer.common.protocol.RequestCode;
import com.alishangtian.mtimer.common.util.JSONUtils;
import com.alishangtian.mtimer.common.util.MtimerUtils;
import com.alishangtian.mtimer.model.core.BrokerWrapper;
import com.alishangtian.mtimer.model.core.MtimerRequest;
import com.alishangtian.mtimer.model.core.MtimerResult;
import com.alishangtian.mtimer.model.metrics.MtimerMetrics;
import com.alishangtian.mtimer.remoting.ConnectFuture;
import com.alishangtian.mtimer.remoting.MtimerCommand;
import com.alishangtian.mtimer.remoting.config.NettyClientConfig;
import com.alishangtian.mtimer.remoting.exception.RemotingConnectException;
import com.alishangtian.mtimer.remoting.exception.RemotingException;
import com.alishangtian.mtimer.remoting.netty.NettyRemotingClient;
import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.channel.Channel;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.JedisCluster;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Desc DefaultMtimerClient
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Builder
@Slf4j
@Data
public class DefaultMtimerClient implements MtimerClient {

    private NettyRemotingClient client;
    private NettyClientConfig config;
    private ClientConfig clientConfig;
    private DefaultChannelEventListener defaultChannelEventListener;
    private MtimerProcessor mtimerProcessor;
    private ExecutorService publicExecutor;
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private JedisCluster jedisCluster;
    @lombok.Builder.Default
    private volatile ConcurrentMap<String, BrokerWrapper> brokerWrapperMap = new ConcurrentHashMap<>();
    @lombok.Builder.Default
    private volatile ConcurrentMap<String, String> zsetBrokerMap = new ConcurrentHashMap<>();

    private volatile String leader;

    private final AtomicInteger connectLeaderFailCounter = new AtomicInteger(0);

    private final ReentrantLock heartBeatLock = new ReentrantLock(true);

    @Override
    public MtimerResult insertMtimer(MtimerRequest mtimerRequest) {
        MtimerResult mtimerResult = MtimerResult.builder().success(false).build();
        try {
            if (StringUtils.isBlank(mtimerRequest.getAppKey()) || StringUtils.isBlank(mtimerRequest.getGroupKey()) || null == mtimerRequest.getCallBackTime()) {
                mtimerResult.setMsg("invalidate mtimer param");
                return mtimerResult;
            }
            mtimerRequest.setClusterName(clientConfig.getClusterName());
            String partitionKey = MtimerUtils.constructClusterPartitions(MtimerUtils.constructPartitionPrefix(String.valueOf(System.currentTimeMillis() % zsetBrokerMap.values().size())), clientConfig.getClusterName());
            mtimerResult.setPartition(partitionKey);
            String host = this.zsetBrokerMap.get(partitionKey);
            if (StringUtils.isBlank(host)) {
                mtimerResult.setMsg("no partition broker for this mtimerRequest");
                return mtimerResult;
            }
            mtimerRequest.setPartition(partitionKey);
            MtimerCommand addMtimerCommand = MtimerCommand.builder().code(RequestCode.CLIENT_ADD_MTIMER_TO_BROKER).load(JSONUtils.toJSONString(mtimerRequest).getBytes()).build();
            MtimerCommand mtimerCommand = this.client.invokeSync(host, addMtimerCommand, 5000L);
            mtimerResult.setSuccess(mtimerCommand.isSuccess());
            mtimerResult.setMsg(mtimerCommand.getRemark());
        } catch (InterruptedException | RemotingException e) {
            log.error("insertMtimer error {}", e.getMessage(), e);
            mtimerResult.setMsg("add Mtimer invokeSync exception");
        } catch (Throwable throwable) {
            log.error("insertMtimer error {}", throwable.getMessage(), throwable);
        }
        return mtimerResult;
    }

    @Override
    public void start() {
        this.start0();
    }

    @Override
    public BrokerStatus brokerStatus() {
        BrokerStatus brokerStatus = BrokerStatus.builder().zsetBrokerMap(zsetBrokerMap).build();
        brokerStatus.setClusterName(clientConfig.getClusterName());
        Set<Map.Entry<String, String>> entrySet = zsetBrokerMap.entrySet();
        Iterator<Map.Entry<String, String>> iterator = entrySet.iterator();
        long retryTotalCount = 0;
        long copyTotalCount = 0;
        while (iterator.hasNext()) {
            String key = iterator.next().getKey();
            brokerStatus.getZsetMap().put(key, jedisCluster.zcard(key));
            String[] keys = key.split(":");
            keys[0] = "copy";
            String copyKey = StringUtils.join(keys, ":");
            long copyCount = jedisCluster.llen(copyKey);
            copyTotalCount += copyCount;
            brokerStatus.getCopyMap().put(copyKey, copyCount);
            keys[0] = "retry";
            String retryKey = StringUtils.join(keys, ":");
            long retryCount = jedisCluster.llen(retryKey);
            retryTotalCount += retryCount;
            brokerStatus.getRetryMap().put(retryKey, retryCount);
            keys[0] = "list";
            String listKey = StringUtils.join(keys, ":");
            brokerStatus.getListMap().put(listKey, jedisCluster.llen(listKey));
        }
        try {
            MtimerCommand mtimerCommand = this.client.invokeSync(leader, MtimerCommand.builder().code(RequestCode.CLIENT_ASK_BROKER_FOR_CLUSTER_METRICS).build(), 5000L);
            brokerStatus.setMetrics(JSONUtils.parseObject(mtimerCommand.getLoad(), MtimerMetrics.class));
            brokerStatus.getMetrics().setCopyTotalCount(copyTotalCount);
            brokerStatus.getMetrics().setRetryTotalCount(retryTotalCount);
        } catch (Exception e) {
            log.error("get metrics error", e);
        }
        return brokerStatus;
    }

    @Override
    public void clearRedisData() {
        Set<Map.Entry<String, String>> entrySet = zsetBrokerMap.entrySet();
        Iterator<Map.Entry<String, String>> iterator = entrySet.iterator();
        while (iterator.hasNext()) {
            String zsetKey = iterator.next().getKey();
            String[] keys = zsetKey.split(":");
            keys[0] = "copy";
            String copyKey = StringUtils.join(keys, ":");
            keys[0] = "retry";
            String retryKey = StringUtils.join(keys, ":");
            keys[0] = "list";
            String listKey = StringUtils.join(keys, ":");
            jedisCluster.del(zsetKey, copyKey, retryKey, listKey);
        }
        try {
            this.client.invokeOneway(this.leader, MtimerCommand.builder().code(RequestCode.CLIENT_ASK_BROKER_FOR_CLEAR_METRICS).build(), 5000L);
        } catch (Exception e) {
            log.error("clear metrics error", e);
        }
    }

    @Override
    public boolean callBack(MtimerCommand mtimerCommand) {
        return false;
    }

    public void start0() {
        client = new NettyRemotingClient(config, defaultChannelEventListener);
        client.registerProcessor(RequestCode.BROKER_ASK_CLIENT_FOR_MTIMER_PROCESSOR,
                InvokeMtimerProcessor.builder().clusterName(clientConfig.getClusterName()).jedisCluster(jedisCluster).mtimerProcessor(mtimerProcessor).build(),
                publicExecutor);
        client.start();
        scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> askLeaderForBrokerTopology(), 0L, clientConfig.getAskLeaderAndHeartBeatToFollowerInterval(), TimeUnit.MILLISECONDS);
    }

    private void askLeaderForBrokerTopology() {
        try {
            leader = jedisCluster.get(MtimerUtils.constructLeaderKey(clientConfig.getClusterName()));
            if (StringUtils.isBlank(leader)) {
                log.warn("there is no mtimer leader exists");
                return;
            }
            Channel channel = this.defaultChannelEventListener.getChannel(leader);
            if (null == channel || !channel.isActive()) {
                connectBroker(leader);
            }
            MtimerCommand request = MtimerCommand.builder().code(RequestCode.CLIENT_ASK_LEADER_FOR_BROKER_TOPOLOGY)
                    .load(JSONUtils.toJSONString(MtimerRequest.builder().groupKey(clientConfig.getGroupKey()).appKey(clientConfig.getAppKey()).build()).getBytes())
                    .build();
            MtimerCommand response = this.client.invokeSync(this.leader, request, 5000L);
            this.brokerWrapperMap = JSONUtils.parseObject(response.getLoad(), new TypeReference<ConcurrentMap<String, BrokerWrapper>>() {
            });
            initBrokerTopology();
            heartBeatToFollowerBroker();
        } catch (InterruptedException e) {
            log.error("askLeaderForBrokerTopology error {}", e.getMessage(), e);
        } catch (RemotingException e) {
            log.error("askLeaderForBrokerTopology error {}", e.getMessage(), e);
        } catch (Throwable throwable) {
            log.error("askLeaderForBrokerTopology error {}", throwable.getMessage(), throwable);
        }
    }

    /**
     * @Description keepHeartBeatToFollowerBroker
     * @Date 2020/7/24 下午3:01
     * @Author maoxiaobing
     **/
    private void heartBeatToFollowerBroker() {
        try {
            this.brokerWrapperMap.forEach((s, brokerWrapper) -> {
                if (!s.equals(this.leader)) {
                    Channel channel = this.defaultChannelEventListener.getChannel(s);
                    if (null == channel || !channel.isActive()) {
                        try {
                            connectBroker(s);
                        } catch (InterruptedException e) {
                            log.error("keepHeartBeatToFollowerBroker connect broker {} error", s, e);
                        } catch (RemotingException e) {
                            log.error("keepHeartBeatToFollowerBroker connect broker {} error", s, e);
                        }
                    }
                    doHeartBeatToFollowerBroker(brokerWrapper);
                }
            });
        } catch (Exception e) {
            log.error("keepHeartBeatToBroker error {}", e.getMessage(), e);
        }
    }

    private void doHeartBeatToFollowerBroker(BrokerWrapper brokerWrapper) {
        MtimerCommand request = MtimerCommand.builder().code(RequestCode.CLIENT_HEART_BEAT_TO_BROKER)
                .load(JSONUtils.toJSONString(MtimerRequest.builder().groupKey(clientConfig.getGroupKey()).appKey(clientConfig.getAppKey()).build()).getBytes())
                .build();
        try {
            this.client.invokeOneway(brokerWrapper.getAddr(), request, 5000L);
            log.info("heart beat to broker {} success", brokerWrapper.getAddr());
        } catch (InterruptedException e) {
            log.error("doHeartBeatToFollowerBroker {} error {}", brokerWrapper.getAddr(), e.getMessage(), e);
        } catch (RemotingException e) {
            log.error("doHeartBeatToFollowerBroker {} error {}", brokerWrapper.getAddr(), e.getMessage(), e);
        } catch (Throwable throwable) {
            log.error("doHeartBeatToFollowerBroker {} error {}", brokerWrapper.getAddr(), throwable.getMessage(), throwable);
        }
    }

    /**
     * initBrokerTopology
     */
    private void initBrokerTopology() {
        try {
            this.brokerWrapperMap.forEach((addr, brokerWrapper) -> brokerWrapper.getServeKeys().forEach(s -> {
                zsetBrokerMap.put(s, addr);
            }));
            log.info("partition topology is {}", JSONUtils.toJSONString(zsetBrokerMap));
        } catch (Exception exception) {
            log.error("initBrokerTopology error {}", exception.getMessage(), exception);
        }
    }

    /**
     * connectBroker
     *
     * @param host
     * @throws InterruptedException
     * @throws RemotingConnectException
     */
    public void connectBroker(String host) throws InterruptedException, RemotingConnectException {
        final ConnectFuture connectFuture = ConnectFuture.builder().build();
        this.client.connect(host).addListener(future -> {
            if (future.isSuccess()) {
                this.defaultChannelEventListener.addCountdownLatch(host, connectFuture.getCountDownLatch());
            } else {
                connectFuture.connectError(host);
            }
        });
        connectFuture.await();
        if (null != connectFuture.getRemotingConnectException()) {
            throw connectFuture.getRemotingConnectException();
        }
        log.info("connect {} success", host);
    }

}
