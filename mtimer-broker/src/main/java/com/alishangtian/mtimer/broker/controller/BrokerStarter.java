package com.alishangtian.mtimer.broker.controller;

import com.alishangtian.mtimer.broker.config.BrokerConfig;
import com.alishangtian.mtimer.common.protocol.RequestCode;
import com.alishangtian.mtimer.common.util.JSONUtils;
import com.alishangtian.mtimer.common.util.MtimerUtils;
import com.alishangtian.mtimer.core.config.ScannerConfig;
import com.alishangtian.mtimer.core.service.MtimerService;
import com.alishangtian.mtimer.core.starter.ScanStarter;
import com.alishangtian.mtimer.enums.BrokerStatusEnum;
import com.alishangtian.mtimer.enums.RoleEnum;
import com.alishangtian.mtimer.exception.AddMtimerException;
import com.alishangtian.mtimer.model.core.BrokerWrapper;
import com.alishangtian.mtimer.model.core.MtimerRequest;
import com.alishangtian.mtimer.model.core.MtimerResult;
import com.alishangtian.mtimer.model.metrics.MtimerBrokerMetrics;
import com.alishangtian.mtimer.processor.*;
import com.alishangtian.mtimer.remoting.ConnectFuture;
import com.alishangtian.mtimer.remoting.MtimerCommand;
import com.alishangtian.mtimer.remoting.common.MtimerUtil;
import com.alishangtian.mtimer.remoting.config.NettyClientConfig;
import com.alishangtian.mtimer.remoting.config.NettyServerConfig;
import com.alishangtian.mtimer.remoting.exception.RemotingConnectException;
import com.alishangtian.mtimer.remoting.exception.RemotingException;
import com.alishangtian.mtimer.remoting.exception.RemotingSendRequestException;
import com.alishangtian.mtimer.remoting.exception.RemotingTimeoutException;
import com.alishangtian.mtimer.remoting.netty.NettyRemotingClient;
import com.alishangtian.mtimer.remoting.netty.NettyRemotingServer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Stopwatch;
import io.netty.channel.Channel;
import io.netty.util.HashedWheelTimer;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Desc BrokerStarter
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Service
@Log4j2
@Data
public class BrokerStarter {

    /**
     * field
     */
    @Autowired
    private NettyServerConfig nettyServerConfig;
    @Autowired
    private NettyClientConfig nettyClientConfig;
    @Autowired
    private ClientChannelProcessor clientChannelProcessor;
    @Autowired
    private JedisCluster jedisCluster;
    @Autowired
    private BrokerConfig brokerConfig;

    @Autowired
    private ScannerConfig scannerConfig;

    @Autowired
    private MtimerService mtimerService;

    private HashedWheelTimer timer = new HashedWheelTimer(new ThreadFactory() {
        AtomicLong threadCount = new AtomicLong();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName("broker-timer-thread-pool-" + threadCount.incrementAndGet());
            return thread;
        }
    }, 100L, TimeUnit.MILLISECONDS, 512);

    private String leaderKey;
    private NettyRemotingServer server;
    private NettyRemotingClient client;
    private ScanStarter scanStarter;
    private ServerChannelProcessor serverChannelProcessor;
    private volatile String hostAddr;
    private volatile String leaderHost;
    private volatile boolean leader = false;

    private ConcurrentMap<String, BrokerWrapper> brokerWrapperMap = new ConcurrentHashMap<>();
    private ReentrantLock requestKeyLock = new ReentrantLock(true);
    private ReentrantLock fullTopologyCheckLock = new ReentrantLock(true);
    private AtomicLong follwerKeepFailCount = new AtomicLong(0);
    private AtomicLong follwerJoinClusterFailCount = new AtomicLong(0);
    private AtomicBoolean waitingFollowerTopology = new AtomicBoolean(false);
    private AtomicInteger leaderWaitingTologyCount = new AtomicInteger(0);
    /**
     * final static
     */
    private static final int PROCESSORS = Runtime.getRuntime().availableProcessors();
    private static final int CORE_SIZE = PROCESSORS;
    private static final int MAX_SIZE = CORE_SIZE + 4;
    private static final String OK = "OK";
    private static final int LEADER_TICK = 10;
    private static final int MIN_WORKER_THREAD_COUNT = 8;
    private static final int MIN_SCHEDULE_WORKER_THREAD_COUNT = 4;
    private static final long TRIGGER_COST_WARN_THRESHOLD = 100L;
    private static final long TRIGGER_RPC_SEND_TIMEOUT = 5000L;
    private static final long LEADER_WAITING_TOPOLOGY_THRESHOLD = 5;
    private static final long NORMAL_RPC_SEND_TIMEOUT = 5000L;

    /**
     * metrics
     **/
    public volatile AtomicLong brokerAddTimerSuccessCounter = new AtomicLong();
    public volatile AtomicLong brokerTriggerdTimerCounter = new AtomicLong();
    public volatile AtomicLong brokerTriggerdTimerSuccessCounter = new AtomicLong();


    private ExecutorService executorService = new ThreadPoolExecutor(CORE_SIZE < MIN_WORKER_THREAD_COUNT ? MIN_WORKER_THREAD_COUNT : CORE_SIZE, MAX_SIZE < MIN_WORKER_THREAD_COUNT ? MIN_WORKER_THREAD_COUNT : MAX_SIZE, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1024), new ThreadFactory() {
        AtomicLong num = new AtomicLong();

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "broker-processor-pool-thread-" + num.getAndIncrement());
        }
    });

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(CORE_SIZE < MIN_SCHEDULE_WORKER_THREAD_COUNT ? MIN_SCHEDULE_WORKER_THREAD_COUNT : CORE_SIZE, new ThreadFactory() {
        AtomicLong num = new AtomicLong();

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "borker-schedule-pool-thread-" + num.getAndIncrement());
        }
    });

    @PostConstruct
    public void start() {
        leaderKey = MtimerUtils.constructLeaderKey(brokerConfig.getClusterName());
        try {
            hostAddr = (StringUtils.isBlank(brokerConfig.getHost()) ? MtimerUtil.getLocalAddress() : brokerConfig.getHost())
                    + ":" + nettyServerConfig.getListenPort();
        } catch (Exception e) {
            log.error("get hostAddr error {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
        log.info("mtimer broker start info:[{}]", hostAddr);
        // start server
        serverChannelProcessor = new ServerChannelProcessor(this);
        server = new NettyRemotingServer(nettyServerConfig, serverChannelProcessor);
        server.registerProcessor(RequestCode.CLIENT_ADD_MTIMER_TO_BROKER, new AddMtimerProcessor(this), executorService);
        server.registerProcessor(RequestCode.CLIENT_HEART_BEAT_TO_BROKER, serverChannelProcessor, executorService);
        server.registerProcessor(RequestCode.CLIENT_ASK_LEADER_FOR_BROKER_TOPOLOGY, serverChannelProcessor, executorService);
        server.registerProcessor(RequestCode.BROKER_HEART_BEAT_TO_CONTROLLER, new BrokerHeartBeatToControllerProcessor(this), executorService);
        server.registerProcessor(RequestCode.BROKER_ASK_FOR_KEYLIST_FROM_CONTROLLER, new BrokerRequestKeysToControllerProcessor(this), executorService);
        server.registerProcessor(RequestCode.LEADER_ASK_FOR_SERVEKEYS_REBLANCE, new LeaderAskBrokerForRebalanceProcessor(this), executorService);
        server.registerProcessor(RequestCode.NEW_LEADER_ASK_FOR_SERVEKEYS_REBLANCE, new NewLeaderAskBrokerForRebalanceProcessor(this), executorService);
        server.registerProcessor(RequestCode.CLIENT_ASK_BROKER_FOR_CLUSTER_METRICS, new ClientBrokerMetricsProcessor(this), executorService);
        server.registerProcessor(RequestCode.BROKER_ASK_BROKER_FOR_CLUSTER_METRICS, new BrokerBrokerMetricsProcessor(this), executorService);
        server.registerProcessor(RequestCode.BROKER_ASK_BROKER_FOR_CLEAR_METRICS, new BrokerBrokerClearMetricsProcessor(this), executorService);
        server.registerProcessor(RequestCode.CLIENT_ASK_BROKER_FOR_CLEAR_METRICS, new ClientBrokerClearMetricsProcessor(this), executorService);
        server.start();
        //start client
        client = new NettyRemotingClient(nettyClientConfig, clientChannelProcessor);
        client.start();
        //init scanner
        initScanner();
        //join cluster and start scaner
        List<String> keys = joinCluster();
        if (isLeader() && waitingFollowerTopology.get()) {
            log.info("I (the leader) am waiting for topology from follower");
            return;
        }
        if (waitingFollowerTopology.get()) {
            log.info("The leader is waiting for topology from follower");
            return;
        }
        if (null == keys || keys.size() == 0) {
            throw new RuntimeException("no more keys can serve");
        }
        if (brokerConfig.isStartScanner()) {
            scanStarter.startScaner(keys);
        }
    }

    /**
     * 添加定时触发任务
     *
     * @Description 添加定时触发任务
     * @Date 2020/6/23 下午3:40
     * @Author maoxiaobing
     **/
    public MtimerResult addMtimer(MtimerCommand request) throws AddMtimerException {
        try {
            MtimerRequest mtimerRequest = JSONUtils.parseObject(request.getLoad(), MtimerRequest.class);
            return mtimerService.addMtimer(mtimerRequest);
        } catch (Exception e) {
            log.error("addMtimer error {}", e.getMessage(), e);
            throw new AddMtimerException(e.getMessage(), e);
        }
    }

    /**
     * @Description 触发定时任务
     * @Date 2020/7/21 下午7:58
     * @Author maoxiaobing
     **/
    public boolean triggerMtimer(Channel channel, MtimerRequest mtimerRequest) {
        try {
            MtimerCommand request = MtimerCommand.builder().code(RequestCode.BROKER_ASK_CLIENT_FOR_MTIMER_PROCESSOR).load(JSONUtils.toJSONString(mtimerRequest).getBytes()).build();
            this.brokerTriggerdTimerCounter.incrementAndGet();
            Stopwatch stopwatch = Stopwatch.createStarted();
            MtimerCommand response = this.server.invokeSync(channel, request, TRIGGER_RPC_SEND_TIMEOUT);
            long cost = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            if (cost > TRIGGER_COST_WARN_THRESHOLD) {
                log.warn("mtimer triggered cost:{}ms result:{}", stopwatch.elapsed(TimeUnit.MILLISECONDS),
                        response.isSuccess());
            }
            try {
                if (response.isSuccess()) {
                    this.brokerTriggerdTimerSuccessCounter.incrementAndGet();
                }
            } catch (Throwable t) {
                log.error("record mrtrics info error {}", t.getMessage(), t);
            }
            return response.isSuccess();
        } catch (InterruptedException | RemotingSendRequestException | RemotingTimeoutException e) {
            log.error("triggerMtimer error {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * @Description
     * @Author maoxiaobing
     **/
    public void cleanHashWheeledData() {
        this.scanStarter.cleanHashWheeledData();
    }

    /**
     * @Description getMetrics
     * @Date 2020/8/16 下午4:55
     * @Author maoxiaobing
     **/
    public MtimerBrokerMetrics getMetrics() {
        MtimerBrokerMetrics metrics = MtimerBrokerMetrics.builder()
                .brokerAddSuccessCount(brokerAddTimerSuccessCounter.longValue())
                .brokerTriggerdCount(brokerTriggerdTimerCounter.longValue())
                .brokerTriggerdSuccessCount(brokerTriggerdTimerSuccessCounter.longValue())
                .brokerAddress(this.hostAddr)
                .role(leader ? RoleEnum.LEADER.name() : RoleEnum.FOLLOWER.name())
                .status(BrokerStatusEnum.ALIVE.name())
                .build();
        return metrics;
    }

    /**
     * @Description getMetrics
     * @Date 2020/8/16 下午4:55
     * @Author maoxiaobing
     **/
    public void clearMetrics() {
        brokerAddTimerSuccessCounter.set(0);
        brokerTriggerdTimerCounter.set(0);
        brokerTriggerdTimerSuccessCounter.set(0);
        cleanHashWheeledData();
    }

    /**
     * @Description getActiveClient
     * @Date 2020/8/16 上午10:16
     * @Author maoxiaobing
     **/
    public Map<String, List<String>> getActiveClient() {
        return this.serverChannelProcessor.getActiveClient();
    }

    /**
     * @Description getMetricsFromBroker
     * @Date 2020/8/16 下午5:44
     * @Author maoxiaobing
     **/
    public MtimerBrokerMetrics getMetricsFromBroker(String folowerAddr) {
        try {
            Channel channel = this.clientChannelProcessor.getChannel(folowerAddr);
            if (null == channel || !channel.isActive()) {
                connectHost(folowerAddr);
            }
            MtimerCommand command = this.client.invokeSync(folowerAddr, MtimerCommand.builder().code(RequestCode.BROKER_ASK_BROKER_FOR_CLUSTER_METRICS).build(), 5000L);
            if (command.isSuccess()) {
                return JSONUtils.parseObject(command.getLoad(), MtimerBrokerMetrics.class);
            } else {
                return MtimerBrokerMetrics.builder().status(BrokerStatusEnum.ALIVE.name()).build();
            }
        } catch (InterruptedException e) {
            return MtimerBrokerMetrics.builder().status(BrokerStatusEnum.UNKNOWN.name()).build();
        } catch (RemotingException e) {
            return MtimerBrokerMetrics.builder().status(BrokerStatusEnum.DEAD.name()).build();
        }
    }

    /**
     * @Description clearBrokerMetrics
     * @Date 2020/8/16 下午5:44
     * @Author maoxiaobing
     **/
    public void clearBrokerMetrics(String folowerAddr) {
        try {
            Channel channel = this.clientChannelProcessor.getChannel(folowerAddr);
            if (null == channel || !channel.isActive()) {
                connectHost(folowerAddr);
            }
            this.client.invokeOneway(folowerAddr, MtimerCommand.builder().code(RequestCode.BROKER_ASK_BROKER_FOR_CLEAR_METRICS).build(), 5000L);
        } catch (Exception e) {
            log.error("{}", e.getLocalizedMessage(), e);
        }
    }

    /**
     * @Description
     * @Date 2020/6/23 下午3:39
     * @Author maoxiaobing
     **/
    private List<String> joinCluster() {
        String result = jedisCluster.set(leaderKey, hostAddr, SetParams.setParams().nx().ex(10));
        if (OK.equals(result)) {
            leader = true;
            waitingFollowerTopology.compareAndSet(false, true);
            scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> keepLeading(), 0L, brokerConfig.getKeepLeadingInterval(), TimeUnit.MILLISECONDS);
            scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> checkFollower(), 0L, brokerConfig.getCheckFollowerInterval(), TimeUnit.MILLISECONDS);
            return null;
        } else {
            leaderHost = jedisCluster.get(leaderKey);
            log.info("hostAddr:{},leaderHost:{}", this.hostAddr, this.leaderHost);
            // 主迅速重启
            if (this.leaderHost.equals(this.hostAddr)) {
                waitingFollowerTopology.compareAndSet(false, true);
                scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> keepLeading(), 0L, brokerConfig.getKeepLeadingInterval(), TimeUnit.MILLISECONDS);
                scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> checkFollower(), 0L, brokerConfig.getCheckFollowerInterval(), TimeUnit.MILLISECONDS);
                leader = true;
                return null;
            }
            leader = false;
            try {
                // request keys
                List<String> keys = null;
                connectHost(leaderHost);
                MtimerCommand request = MtimerCommand.builder().code(RequestCode.BROKER_ASK_FOR_KEYLIST_FROM_CONTROLLER)
                        .hostAddr(this.hostAddr).build();
                MtimerCommand response = this.client.invokeSync(leaderHost, request, NORMAL_RPC_SEND_TIMEOUT);
                if (response.isWaitingFollowerTopology()) {
                    this.waitingFollowerTopology.compareAndSet(false, true);
                } else if (response.isSuccess()) {
                    keys = JSONUtils.parseObject(response.getLoad(), new TypeReference<List<String>>() {
                    });
                }
                scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> keepFollowing(), 0L, 2000L, TimeUnit.MILLISECONDS);
                return keys;
            } catch (InterruptedException e) {
                log.error("InterruptedException {}", e.getMessage(), e);
            } catch (RemotingConnectException | RemotingSendRequestException | RemotingTimeoutException e) {
                log.error("RemotingException {}", e.getMessage(), e);
                while (this.follwerJoinClusterFailCount.getAndIncrement() < brokerConfig.getJoinClusterFailThreshold()) {
                    try {
                        connectHost(leaderHost);
                        MtimerCommand request = MtimerCommand.builder().code(RequestCode.BROKER_ASK_FOR_KEYLIST_FROM_CONTROLLER)
                                .hostAddr(this.hostAddr).build();
                        MtimerCommand response = this.client.invokeSync(leaderHost, request, NORMAL_RPC_SEND_TIMEOUT);
                        List<String> keys = null;
                        if (response.isWaitingFollowerTopology()) {
                            this.waitingFollowerTopology.compareAndSet(false, true);
                        } else if (response.isSuccess()) {
                            keys = JSONUtils.parseObject(response.getLoad(), new TypeReference<List<String>>() {
                            });
                        }
                        scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> keepFollowing(), 0L, 2000L, TimeUnit.MILLISECONDS);
                        this.follwerJoinClusterFailCount.set(0);
                        return keys;
                    } catch (InterruptedException interruptedException) {
                        log.error("InterruptedException {}", interruptedException.getMessage(), interruptedException);
                    } catch (RemotingConnectException remotingConnectException) {
                        log.error("RemotingConnectException {}", remotingConnectException.getMessage(), remotingConnectException);
                    } catch (RemotingException remotingException) {
                        log.error("RemotingException {}", remotingException.getMessage(), remotingException);
                    }
                    try {
                        Thread.sleep(NORMAL_RPC_SEND_TIMEOUT);
                    } catch (InterruptedException interruptedException) {
                        log.error("InterruptedException {}", interruptedException.getMessage(), interruptedException);
                    }
                }
            }
        }
        return null;
    }

    /**
     * @Description
     * @Date 2020/6/23 下午4:21
     * @Author maoxiaobing
     **/
    private void initScanner() {
        if (null == scanStarter) {
            scanStarter = ScanStarter.builder()
                    .jedisCluster(jedisCluster)
                    .callBackProcessor(mtimerRequest -> serverChannelProcessor.invokeMtimerTrigger(mtimerRequest))
                    .scannerConfig(scannerConfig)
                    .build();
            scanStarter.initThreadpool();
        }
    }

    /**
     * @Description
     * @Date 2020/6/24 下午2:28
     * @Author maoxiaobing
     **/
    private void keepLeading() {
        try {
            jedisCluster.set(leaderKey, hostAddr, SetParams.setParams().ex(LEADER_TICK));
            if (waitingFollowerTopology.get()) {
                timer.newTimeout(timeout -> {
                    if (waitingFollowerTopology.get() && leaderWaitingTologyCount.incrementAndGet() > LEADER_WAITING_TOPOLOGY_THRESHOLD && waitingFollowerTopology.compareAndSet(true, false)) {
                        leaderWaitingTologyCount.set(0);
                        List<String> keys = keyPartitions();
                        this.brokerWrapperMap.put(hostAddr, BrokerWrapper.builder().addr(hostAddr).serveKeys(keys).build());
                        if (brokerConfig.isStartScanner()) {
                            scanStarter.restart(keys);
                        }
                    }
                }, 2000L, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.error("keepLeading error", e);
        }
        log.info("keepLeading over");
    }

    /**
     * @Description
     * @Date 2020/6/24 下午2:28
     * @Author maoxiaobing
     **/
    private void checkFollower() {
        try {
            log.info("brokerWrapperMap [{}]", JSONUtils.toJSONString(this.brokerWrapperMap));
            Set<Map.Entry<String, BrokerWrapper>> keySet = brokerWrapperMap.entrySet();
            Iterator<Map.Entry<String, BrokerWrapper>> iterator = keySet.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, BrokerWrapper> next = iterator.next();
                if (next.getValue().isSelf(this.hostAddr)) {
                    next.getValue().setLastHeartBeat(System.currentTimeMillis());
                } else if (next.getValue().timeout(brokerConfig.getTimeoutThreshold())) {
                    askBrokerForReblance(next.getValue());
                    log.warn("follower {} is timeout", next.getValue().getAddr());
                    iterator.remove();
                }
            }
            log.info("checkFollower over");
        } catch (Exception exception) {
            log.error("checkFollower error [{}]", exception);
        }
    }

    /**
     * @Description askBrokerForReblance
     * @Date 2020/7/14 下午4:44
     * @Author maoxiaobing
     * @todo 引入两阶段提交，保证所有节点rebalance时已经处于暂定扫描状态。
     **/
    private boolean askBrokerForReblance(BrokerWrapper timeoutBrokerWrapper) {
        List<String> keys = timeoutBrokerWrapper.getServeKeys();
        Set<Map.Entry<String, BrokerWrapper>> entrySet = this.brokerWrapperMap.entrySet();
        List<Map.Entry<String, BrokerWrapper>> entryList = new ArrayList<>(entrySet);
        for (String key : keys) {
            Collections.sort(entryList, Comparator.comparingInt(t -> t.getValue().getServeKeys().size()));
            boolean shot = false;
            while (!shot) {
                for (Map.Entry<String, BrokerWrapper> stringBrokerWrapperEntry : entryList) {
                    if (!stringBrokerWrapperEntry.getKey().equals(timeoutBrokerWrapper.getAddr())) {
                        stringBrokerWrapperEntry.getValue().getServeKeys().add(key);
                        shot = true;
                        break;
                    }
                }
            }
        }
        for (Map.Entry<String, BrokerWrapper> stringBrokerWrapperEntry : entrySet) {
            if (!stringBrokerWrapperEntry.getKey().equals(timeoutBrokerWrapper.getAddr())) {
                if (stringBrokerWrapperEntry.getKey().equals(this.hostAddr)) {
                    if (brokerConfig.isStartScanner()) {
                        this.scanStarter.restart(stringBrokerWrapperEntry.getValue().getServeKeys());
                    }
                } else {
                    notifyBrokerForRebalance(stringBrokerWrapperEntry.getValue());
                }
            }
        }
        return true;
    }

    /**
     * @Description newLeaderAskBrokerForReblance
     * @Date 2020/7/14 下午4:44
     * @Author maoxiaobing
     * @todo 引入两阶段提交，保证所有节点rebalance时已经处于暂定扫描状态。
     **/
    private boolean newLeaderAskBrokerForReblance(BrokerWrapper timeoutBrokerWrapper) {
        List<String> keys = timeoutBrokerWrapper.getServeKeys();
        Set<Map.Entry<String, BrokerWrapper>> entrySet = this.brokerWrapperMap.entrySet();
        List<Map.Entry<String, BrokerWrapper>> entryList = new ArrayList<>(entrySet);
        for (String key : keys) {
            Collections.sort(entryList, Comparator.comparingInt(t -> t.getValue().getServeKeys().size()));
            boolean shot = false;
            while (!shot) {
                for (Map.Entry<String, BrokerWrapper> stringBrokerWrapperEntry : entryList) {
                    if (!stringBrokerWrapperEntry.getKey().equals(timeoutBrokerWrapper.getAddr())) {
                        stringBrokerWrapperEntry.getValue().getServeKeys().add(key);
                        shot = true;
                        break;
                    }
                }
            }
        }
        for (Map.Entry<String, BrokerWrapper> stringBrokerWrapperEntry : entrySet) {
            if (!stringBrokerWrapperEntry.getKey().equals(timeoutBrokerWrapper.getAddr())) {
                if (stringBrokerWrapperEntry.getKey().equals(this.hostAddr)) {
                    if (brokerConfig.isStartScanner()) {
                        this.scanStarter.restart(stringBrokerWrapperEntry.getValue().getServeKeys());
                    }
                } else {
                    notifyBrokerForRebalanceWithNewLeader(stringBrokerWrapperEntry.getValue());
                }
            }
        }
        log.warn("brokerWrapperMap del {} for leader Reelection", timeoutBrokerWrapper.getAddr());
        this.brokerWrapperMap.remove(timeoutBrokerWrapper.getAddr());
        return true;
    }

    /**
     * @Description keepFollowing
     * @Date 2020/7/9 下午7:32
     * @Author maoxiaobing
     * @Your-Attention RemotingConnectException mark the failed connection
     **/
    private void keepFollowing() {
        MtimerCommand mtimerCommand = MtimerCommand.builder().code(RequestCode.BROKER_HEART_BEAT_TO_CONTROLLER)
                .hostAddr(this.hostAddr).build();
        if (this.waitingFollowerTopology.get()) {
            mtimerCommand.setLoad(JSONUtils.toJSONString(this.brokerWrapperMap).getBytes());
        }
        try {
            Channel channel = this.clientChannelProcessor.getChannel(this.leaderHost);
            if (null == channel || !channel.isActive()) {
                connectHost(this.leaderHost);
            }
            MtimerCommand response = this.client.invokeSync(this.leaderHost, mtimerCommand, NORMAL_RPC_SEND_TIMEOUT);
            if (response.isWaitingFollowerTopology()) {
                this.waitingFollowerTopology.compareAndSet(false, true);
            } else {
                if (this.waitingFollowerTopology.compareAndSet(true, false)) {
                    MtimerCommand request = MtimerCommand.builder().code(RequestCode.BROKER_ASK_FOR_KEYLIST_FROM_CONTROLLER)
                            .hostAddr(this.hostAddr).build();
                    MtimerCommand askKeysResponse = this.client.invokeSync(leaderHost, request, NORMAL_RPC_SEND_TIMEOUT);
                    if (response.isWaitingFollowerTopology()) {
                        this.waitingFollowerTopology.compareAndSet(false, true);
                    } else if (response.isSuccess()) {
                        List<String> keys = JSONUtils.parseObject(askKeysResponse.getLoad(), new TypeReference<List<String>>() {
                        });
                        if (null != keys && keys.size() > 0) {
                            if (brokerConfig.isStartScanner()) {
                                scanStarter.restart(keys);
                            }
                            log.info("joinCluster getServeKeys from leader {} keys {}", this.leaderHost, JSONUtils.toJSONString(keys));
                        }
                    }
                } else {
                    this.brokerWrapperMap = JSONUtils.parseObject(response.getLoad(), new TypeReference<ConcurrentMap<String, BrokerWrapper>>() {
                    });
                    if (response.getCode() == RequestCode.LEADER_DOSE_NOT_HAS_YOUR_WRAPPER) {
                        MtimerCommand request = MtimerCommand.builder().code(RequestCode.BROKER_ASK_FOR_KEYLIST_FROM_CONTROLLER)
                                .hostAddr(this.hostAddr).build();
                        MtimerCommand keysResponse = this.client.invokeSync(leaderHost, request, NORMAL_RPC_SEND_TIMEOUT);
                        if (keysResponse.isWaitingFollowerTopology()) {
                            this.waitingFollowerTopology.compareAndSet(false, true);
                        } else if (keysResponse.isSuccess()) {
                            List<String> keys = JSONUtils.parseObject(keysResponse.getLoad(), new TypeReference<List<String>>() {
                            });
                            scanStarter.restart(keys);
                        }
                    }
                    log.info("keepfollowing over leader:{} brokerWrapperMap:{}", this.leaderHost, JSONUtils.toJSONString(this.brokerWrapperMap));
                }
            }
        } catch (RemotingConnectException | RemotingSendRequestException e) {
            log.error("keepfollowing error {}", e.getMessage(), e);
            if (this.follwerKeepFailCount.incrementAndGet() > brokerConfig.getLeaderFailThreshold()) {
                if (reElection()) {
                    this.follwerKeepFailCount.set(0);
                }
            }
        } catch (RemotingTimeoutException | InterruptedException e) {
            log.error("keepFollowing error {}", e.getMessage(), e);
        }
    }

    /**
     * @Description reElection
     * @Date 2020/7/16 下午5:00
     * @Author maoxiaobing
     **/
    private boolean reElection() {
        log.info("follower {} start leader election", this.hostAddr);
        String result = jedisCluster.set(leaderKey, hostAddr, SetParams.setParams().nx().ex(60));
        if (OK.equals(result)) {
            log.info("follower {} campaign as leader success", this.hostAddr);
            log.info("new leader {} start work as leader", this.hostAddr);
            this.leader = true;
            String formerLeader = this.leaderHost;
            this.leaderHost = this.hostAddr;
            BrokerWrapper brokerWrapper = this.brokerWrapperMap.get(formerLeader);
            newLeaderAskBrokerForReblance(brokerWrapper);
            restartScheduledThreadPoolExecutor();
            scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> keepLeading(), 0L, brokerConfig.getKeepLeadingInterval(), TimeUnit.MILLISECONDS);
            scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> checkFollower(), 0L, brokerConfig.getCheckFollowerInterval(), TimeUnit.MILLISECONDS);
            return true;
        }
        log.info("follower {} campaign leader failed", this.hostAddr);
        return true;
    }

    /**
     * @Description 心跳更新
     * @Date 2020/7/9 下午7:31
     * @Author maoxiaobing
     **/
    public BrokerWrapper heartBeat(String followerHost) {
        log.info("heartBeating follower:{} --> leader:{}", followerHost, this.hostAddr);
        BrokerWrapper brokerWrapper = this.brokerWrapperMap.get(followerHost);
        if (null != brokerWrapper) {
            brokerWrapper.setLastHeartBeat(System.currentTimeMillis());
            return brokerWrapper;
        }
        return null;
    }

    /**
     * 请求key列表
     *
     * @param channel
     * @return
     */
    public List<String> requestZsetKeys(Channel channel, String brokerAddr) {
        if (!isLeader()) {
            return null;
        }
        requestKeyLock.lock();
        try {
            List<String> keys = new ArrayList<>();
            if (this.brokerWrapperMap.isEmpty()) {
                return keys;
            }
            BrokerWrapper brokerWrapper = this.brokerWrapperMap.getOrDefault(brokerAddr, BrokerWrapper.builder().lastHeartBeat(System.currentTimeMillis()).addr(brokerAddr).build());
            if (null == brokerWrapper.getServeKeys() || brokerWrapper.getServeKeys().isEmpty()) {
                int serveCount = brokerConfig.getPartitionCount() / (this.brokerWrapperMap.size() + 1);
                Set<Map.Entry<String, BrokerWrapper>> entrySet = this.brokerWrapperMap.entrySet();
                List<Map.Entry<String, BrokerWrapper>> entryList = new ArrayList<>(entrySet);
                while (serveCount > 0) {
                    Collections.sort(entryList, (t1, t2) -> t2.getValue().getServeKeys().size() - t1.getValue().getServeKeys().size());
                    if (entryList.get(0).getValue().getServeKeys().size() > 1) {
                        keys.add(entryList.get(0).getValue().getServeKeys().remove(0));
                        serveCount--;
                    }

                }
                brokerWrapper.setServeKeys(keys);
                for (Map.Entry<String, BrokerWrapper> stringBrokerWrapperEntry : entrySet) {
                    if (stringBrokerWrapperEntry.getValue().isSelf(this.hostAddr) && brokerConfig.isStartScanner()) {
                        scanStarter.restart(stringBrokerWrapperEntry.getValue().getServeKeys());
                        continue;
                    }
                    notifyBrokerForRebalance(stringBrokerWrapperEntry.getValue());
                }
                this.brokerWrapperMap.put(brokerAddr, brokerWrapper);
            }
            return brokerWrapper.getServeKeys();
        } finally {
            requestKeyLock.unlock();
        }
    }

    /**
     * @Description
     * @Date 2020/7/3 下午3:56
     * @Author maoxiaobing
     **/
    private List<String> keyPartitions() {
        List<String> partitions = new ArrayList<>(brokerConfig.getPartitionCount());
        for (int i = 0; i < brokerConfig.getPartitionCount(); i++) {
            partitions.add(MtimerUtils.constructClusterPartitions(MtimerUtils.constructPartitionPrefix(String.valueOf(i)), brokerConfig.getClusterName()));
        }
        return partitions;
    }

    /**
     * @Description
     * @Date 2020/7/3 下午7:45
     * @Author maoxiaobing
     **/
    private boolean notifyBrokerForRebalance(BrokerWrapper brokerWrapper) {
        if (brokerWrapper.isSelf(this.hostAddr)) {
            return true;
        }
        try {
            final String brokerAddr = brokerWrapper.getAddr();
            Channel channel = this.clientChannelProcessor.getChannel(brokerAddr);
            if (null == channel || !channel.isActive()) {
                connectHost(brokerWrapper.getAddr());
            }
            MtimerCommand request = MtimerCommand.builder()
                    .code(RequestCode.LEADER_ASK_FOR_SERVEKEYS_REBLANCE)
                    .load(JSONUtils.toJSONString(brokerWrapper).getBytes())
                    .hostAddr(this.hostAddr)
                    .build();
            MtimerCommand response = this.client.invokeSync(brokerWrapper.getAddr(), request, NORMAL_RPC_SEND_TIMEOUT);
            return response.isSuccess();
        } catch (InterruptedException e) {
            log.error("notifyBrokerForRebalance-InterruptedException", e);
        } catch (RemotingConnectException e) {
            log.error("notifyBrokerForRebalance-RemotingConnectException", e);
        } catch (RemotingSendRequestException e) {
            log.error("notifyBrokerForRebalance-RemotingSendRequestException", e);
        } catch (RemotingTimeoutException e) {
            log.error("notifyBrokerForRebalance-RemotingTimeoutException", e);
        }
        return false;
    }

    /**
     * @Description
     * @Date 2020/7/3 下午7:45
     * @Author maoxiaobing
     **/
    private boolean notifyBrokerForRebalanceWithNewLeader(BrokerWrapper brokerWrapper) {
        if (brokerWrapper.isSelf(this.hostAddr)) {
            return true;
        }
        try {
            final String brokerAddr = brokerWrapper.getAddr();
            Channel channel = this.clientChannelProcessor.getChannel(brokerAddr);
            if (null == channel || !channel.isActive()) {
                connectHost(brokerWrapper.getAddr());
            }
            MtimerCommand request = MtimerCommand.builder()
                    .code(RequestCode.NEW_LEADER_ASK_FOR_SERVEKEYS_REBLANCE)
                    .load(JSONUtils.toJSONString(brokerWrapper).getBytes())
                    .hostAddr(this.hostAddr)
                    .build();
            MtimerCommand response = this.client.invokeSync(brokerWrapper.getAddr(), request, NORMAL_RPC_SEND_TIMEOUT);
            return response.isSuccess();
        } catch (InterruptedException e) {
            log.error("notifyBrokerForRebalance-InterruptedException", e);
        } catch (RemotingConnectException e) {
            log.error("notifyBrokerForRebalance-RemotingConnectException", e);
        } catch (RemotingSendRequestException e) {
            log.error("notifyBrokerForRebalance-RemotingSendRequestException", e);
        } catch (RemotingTimeoutException e) {
            log.error("notifyBrokerForRebalance-RemotingTimeoutException", e);
        }
        return false;
    }

    /**
     * leaderAskForRebalance
     *
     * @param request
     * @return
     */
    public boolean leaderAskForRebalance(MtimerCommand request) {
        BrokerWrapper brokerWrapper = JSONUtils.parseObject(request.getLoad(), BrokerWrapper.class);
        BrokerWrapper localWrapper = this.brokerWrapperMap.get(this.hostAddr);
        if (null != localWrapper) {
            localWrapper.setServeKeys(brokerWrapper.getServeKeys());
        } else {
            this.brokerWrapperMap.put(brokerWrapper.getAddr(), brokerWrapper);
        }
        if (brokerConfig.isStartScanner()) {
            scanStarter.restart(brokerWrapper.getServeKeys());
        }
        if (!this.leaderHost.equals(request.getHostAddr())) {
            this.leaderHost = request.getHostAddr();
            restartScheduledThreadPoolExecutor();
            scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> keepFollowing(), 0L, brokerConfig.getHeartbeatInterval(), TimeUnit.MILLISECONDS);
        }
        return true;
    }

    /**
     * 重启线程池
     *
     * @return
     */
    private boolean restartScheduledThreadPoolExecutor() {
        try {
            this.scheduledThreadPoolExecutor.shutdownNow();
            this.scheduledThreadPoolExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("RestartScheduledThreadPoolExecutor warn {}", e.getMessage());
        }
        this.scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(CORE_SIZE, new ThreadFactory() {
            AtomicLong num = new AtomicLong();

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "borker-schedule-pool-thread-" + num.getAndIncrement());
            }
        });
        return true;
    }

    /**
     * @Description connectHost
     * @Date 2020/7/22 上午11:50
     * @Author maoxiaobing
     **/
    public void connectHost(final String host) throws InterruptedException, RemotingConnectException {
        final ConnectFuture connectFuture = ConnectFuture.builder().build();
        this.client.connect(host).addListener(future -> {
            if (future.isSuccess()) {
                log.info("connect broker {} send success", host);
                this.clientChannelProcessor.addCountdownLatch(host, connectFuture.getCountDownLatch());
            } else {
                connectFuture.connectError(host);
            }
        });
        connectFuture.await();
        if (null != connectFuture.getRemotingConnectException()) {
            throw connectFuture.getRemotingConnectException();
        }
        log.info("connect broker {} success", host);
    }

    /**
     * @Description hasFullTopology
     * @Date 2020/7/27 下午6:40
     * @Author maoxiaobing
     **/
    public void checkFullTopology(MtimerCommand mtimerCommand) {
        fullTopologyCheckLock.lock();
        try {
            Set<String> keySet = new HashSet<>();
            ConcurrentMap<String, BrokerWrapper> maps = JSONUtils.parseObject(mtimerCommand.getLoad(), new TypeReference<ConcurrentMap<String, BrokerWrapper>>() {
            });
            Set<Map.Entry<String, BrokerWrapper>> entries = maps.entrySet();
            for (Map.Entry<String, BrokerWrapper> entry : entries) {
                keySet.addAll(entry.getValue().getServeKeys());
            }
            if (keySet.size() == brokerConfig.getPartitionCount() && this.waitingFollowerTopology.compareAndSet(true, false)) {
                brokerWrapperMap = maps;
                if (brokerConfig.isStartScanner()) {
                    scanStarter.restart(brokerWrapperMap.get(this.hostAddr).getServeKeys());
                }
            }
        } finally {
            fullTopologyCheckLock.unlock();
        }
    }
}
