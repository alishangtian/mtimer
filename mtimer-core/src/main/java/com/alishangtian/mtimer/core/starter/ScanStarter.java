package com.alishangtian.mtimer.core.starter;

import com.alishangtian.mtimer.core.config.ScannerConfig;
import com.alishangtian.mtimer.core.processor.CallBackProcessor;
import com.alishangtian.mtimer.core.processor.CheckCopyWorker;
import com.alishangtian.mtimer.core.processor.ScanListToCopyWorker;
import com.alishangtian.mtimer.core.processor.ScanZsetWorker;
import io.netty.util.HashedWheelTimer;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisCluster;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Desc ScanStarter
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Builder
@Slf4j
public class ScanStarter {

    private static final int PROCESSORS = Runtime.getRuntime().availableProcessors();
    private static final int CORE_SIZE = PROCESSORS * 2;
    private static final int MAX_SIZE = CORE_SIZE + CORE_SIZE / 2;
    private static final int MIN_SCHEDULE_WORKER_THREAD_COUNT = 4;
    private static final int THREAD_TIMEOUT_SECONDS = 60;
    private CallBackProcessor callBackProcessor;
    private static final long SCAN_ZSET_INTERVAL_DEPLAY = 0L;
    private static final long SCAN_ZSET_INTERVAL = 1000L;
    private static final long SCAN_LIST_TO_COPY_INTERVAL_DEPLAY = 0L;
    private static final long SCAN_LIST_TO_COPY_INTERVAL = 1000L;

    private List<String> zsetKeys;
    private JedisCluster jedisCluster;
    private ScannerConfig scannerConfig;

    private ThreadPoolExecutor scanZsetExecutor;
    private ThreadPoolExecutor scanListExecutor;
    private ThreadPoolExecutor triggerExecutor;
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private HashedWheelTimer hashedWheelTimer;

    public void initThreadpool() {
        scanZsetExecutor = new ThreadPoolExecutor(scannerConfig.getScanZsetThreadPoolCoreSize() < CORE_SIZE ? CORE_SIZE : scannerConfig.getScanZsetThreadPoolCoreSize(), scannerConfig.getScanZsetThreadPoolMaxSize() < MAX_SIZE ? MAX_SIZE : scannerConfig.getScanZsetThreadPoolMaxSize(), THREAD_TIMEOUT_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(scannerConfig.getScanZsetThreadPoolQueueMaxSize()),
                new ThreadFactory() {
                    final AtomicLong INDEX = new AtomicLong();

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "scanner-zset-worker-pool-" + INDEX.getAndIncrement());
                    }
                });
        scanListExecutor = new ThreadPoolExecutor(scannerConfig.getScanListThreadPoolCoreSize() < CORE_SIZE ? CORE_SIZE : scannerConfig.getScanListThreadPoolCoreSize(), scannerConfig.getScanListThreadPoolMaxSize() < MAX_SIZE ? MAX_SIZE : scannerConfig.getScanListThreadPoolMaxSize(), THREAD_TIMEOUT_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(scannerConfig.getScanListThreadPoolQueueMaxSize()),
                new ThreadFactory() {
                    final AtomicLong INDEX = new AtomicLong();

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "scanner-list-worker-pool-" + INDEX.getAndIncrement());
                    }
                });
        triggerExecutor = new ThreadPoolExecutor(scannerConfig.getTriggerThreadPoolCoreSize() < CORE_SIZE ? CORE_SIZE : scannerConfig.getTriggerThreadPoolCoreSize(), scannerConfig.getTriggerThreadPoolMaxSize() < MAX_SIZE ? MAX_SIZE : scannerConfig.getTriggerThreadPoolMaxSize(), THREAD_TIMEOUT_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(scannerConfig.getTriggerThreadPoolQueueMaxSize()),
                new ThreadFactory() {
                    final AtomicLong INDEX = new AtomicLong();

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "trigger-mtimer-pool-" + INDEX.getAndIncrement());
                    }
                });
    }

    /**
     * @Description startScaner
     * @Date 2020/7/29 下午6:00
     * @Author maoxiaobing
     **/
    public void startScaner(List<String> serveKeys) {
        this.zsetKeys = serveKeys;
        startTimerAndExcutors();
        checkCopyList(new CountDownLatch(zsetKeys.size()));
        startScanZset();
        startScanList();
        log.info("scaner start success");
    }

    /**
     * @Description reStartScaner
     * @Date 2020/7/29 下午6:00
     * @Author maoxiaobing
     **/
    public void reStartScaner(List<String> serveKeys) {
        this.zsetKeys = serveKeys;
        checkCopyList(new CountDownLatch(zsetKeys.size()));
        startScanZset();
        startScanList();
        log.info("scaner restart success");
    }

    private void startScanZset() {
        scheduledThreadPoolExecutor.scheduleAtFixedRate(ScanZsetWorker.builder().executorService(scanZsetExecutor).jedisCluster(jedisCluster).keySets(zsetKeys).build(),
                SCAN_ZSET_INTERVAL_DEPLAY,
                SCAN_ZSET_INTERVAL,
                TimeUnit.MILLISECONDS);
    }

    private void startScanList() {
        scheduledThreadPoolExecutor.scheduleAtFixedRate(ScanListToCopyWorker.builder().jedisCluster(jedisCluster).keySets(zsetKeys)
                        .publicExecutor(scanListExecutor).triggerExecutor(triggerExecutor).timer(hashedWheelTimer).callBackProcessor(callBackProcessor).build(),
                SCAN_LIST_TO_COPY_INTERVAL_DEPLAY,
                SCAN_LIST_TO_COPY_INTERVAL,
                TimeUnit.MILLISECONDS);
    }

    private void checkCopyList(CountDownLatch countDownLatch) {
        CheckCopyWorker worker = CheckCopyWorker.builder()
                .jedisCluster(jedisCluster)
                .executorService(scanZsetExecutor)
                .keySets(zsetKeys)
                .hashedWheelTimer(hashedWheelTimer)
                .callBackProcessor(callBackProcessor)
                .countDownLatch(countDownLatch).build();
        scanListExecutor.submit(worker);
        try {
            countDownLatch.await(5000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.warn("checkCopyList time out or failed {}", e.getMessage(), e);
        }
    }

    public void cleanHashWheeledData() {
        restart(zsetKeys);
    }

    public void shutdownTimerAndExcutors() {
        /**
         * 关闭定时轮
         */
        if (null != hashedWheelTimer) {
            hashedWheelTimer.stop();
        }
        /**
         * 关闭zset和list扫描定时任务
         */
        if (null != scheduledThreadPoolExecutor) {
            scheduledThreadPoolExecutor.shutdownNow();
            try {
                scheduledThreadPoolExecutor.awaitTermination(5000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.error("scan starter shutdown error {}", e.getMessage(), e);
            }
        }
    }

    public void startTimerAndExcutors() {
        hashedWheelTimer = new HashedWheelTimer(new ThreadFactory() {
            AtomicLong threadCount = new AtomicLong();

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY + 3);
                thread.setName("hashedWheelTimer-consumer-thread-" + threadCount.getAndIncrement());
                return thread;
            }
        }, 100L, TimeUnit.MILLISECONDS, 512);
        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(PROCESSORS < MIN_SCHEDULE_WORKER_THREAD_COUNT ? MIN_SCHEDULE_WORKER_THREAD_COUNT : PROCESSORS, new ThreadFactory() {
            AtomicLong nums = new AtomicLong();

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "scanner-scheduled-pool-" + nums.getAndIncrement());
            }
        });
    }

    public void restartTimerAndExcutors() {
        this.shutdownTimerAndExcutors();
        this.startTimerAndExcutors();
    }

    public void restart(List<String> serveKeys) {
        this.restartTimerAndExcutors();
        this.reStartScaner(serveKeys);
    }
}
