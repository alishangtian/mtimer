package com.alishangtian.mtimer.core.processor;

import com.alishangtian.mtimer.common.util.JSONUtils;
import com.alishangtian.mtimer.model.core.MtimerRequest;
import io.netty.util.HashedWheelTimer;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.JedisCluster;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Desc CheckCopyWorker
 * @Time 2020/08/30
 * @Author alishangtian
 * TODO 备份队列元素在遍历时有可能遇上到期触发删除的问题，会导致start和end不准，丢失部分元素。
 */
@Builder
@Slf4j
public class CheckCopyWorker implements Runnable {

    private HashedWheelTimer hashedWheelTimer;
    private JedisCluster jedisCluster;
    private List<String> keySets;
    private CallBackProcessor callBackProcessor;
    private CountDownLatch countDownLatch;
    private static final long PAGE_SIZE = 20;
    private List<String> keys;
    private ExecutorService executorService;

    @Override
    public void run() {
        keySets.forEach(key -> {
            try {
                executorService.submit(() -> {
                    try {
                        String[] keyArray = StringUtils.split(key, ":");
                        String copyKey = zsetToCopyListKey(keyArray);
                        String retryKey = zsetToRetryListKey(keyArray);
                        long start = 0;
                        long end = start + PAGE_SIZE;
                        while (true) {
                            List<String> results = jedisCluster.lrange(copyKey, start, end);
                            if (null != results && results.size() > 0) {
                                results.forEach(value -> {
                                    final MtimerRequest mtimerRequest = JSONUtils.parseObject(value, MtimerRequest.class);
                                    long time = System.currentTimeMillis() - mtimerRequest.getCallBackTime();
                                    if (time >= 0) {
                                        log.warn("checkcopyworker mtimer triggered delay:{}ms for redis", time);
                                        try {
                                            executorService.submit(() -> {
                                                try {
                                                    if (callBackProcessor.trigger(mtimerRequest)) {
                                                        deleteCopy(copyKey, value);
                                                    } else {
                                                        jedisCluster.lpush(retryKey, value);
                                                        log.warn("CheckCopyWorker timer trigger failed send into retry queue {}", JSONUtils.toJSONString(mtimerRequest));
                                                    }
                                                } catch (Exception e) {
                                                    jedisCluster.lpush(retryKey, value);
                                                    log.error("CheckCopyWorker trigger error for redis send into retry queue {}", e.getMessage(), e);
                                                }
                                            });
                                        } catch (Exception e) {
                                            jedisCluster.lpush(retryKey, value);
                                            log.error("CheckCopyWorker submit error for redis {}", e.getMessage(), e);
                                        }
                                    } else {
                                        hashedWheelTimer.newTimeout(timeout -> {
                                            long delay = System.currentTimeMillis() - mtimerRequest.getCallBackTime();
                                            if (delay > 150) {
                                                log.warn("CheckCopyWorker mtimer triggered delay:{}ms for hashedwaheeledtimer", System.currentTimeMillis() - mtimerRequest.getCallBackTime());
                                            }
                                            try {
                                                executorService.submit(() -> {
                                                    try {
                                                        if (callBackProcessor.trigger(mtimerRequest)) {
                                                            deleteCopy(copyKey, value);
                                                        } else {
                                                            jedisCluster.lpush(retryKey, value);
                                                            log.warn("CheckCopyWorker timer trigger failed send into retry queue {}", JSONUtils.toJSONString(mtimerRequest));
                                                        }
                                                    } catch (Exception e) {
                                                        jedisCluster.lpush(retryKey, value);
                                                        log.error("CheckCopyWorker trigger error for hashedwaheeledtimer send into retry queue {}", e.getMessage(), e);
                                                    }
                                                });
                                            } catch (Exception e) {
                                                jedisCluster.lpush(retryKey, value);
                                                log.error("CheckCopyWorker submit error for hashedwaheeledtimer {}", e.getMessage(), e);
                                            }
                                        }, mtimerRequest.getCallBackTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                                    }
                                });
                                if (results.size() < PAGE_SIZE + 1) {
                                    break;
                                }
                                start = end;
                                end += PAGE_SIZE;
                                continue;
                            }
                            break;
                        }
                        log.info("checkCopyList worker execute success");
                    } catch (Exception e) {
                        log.error("CheckCopyWorker execute error {}", e.getMessage(), e);
                    } finally {
                        countDownLatch.countDown();
                    }
                });
            } catch (Exception e) {
                log.error("CheckCopyWorker submit key {} error {}", key, e.getMessage(), e);
                countDownLatch.countDown();
            }
        });
        log.info("CheckCopyWorker started");
    }

    /**
     * @Author maoxiaobing
     * @Description listToCopyListKey
     * @Date 2020/6/18
     * @Param [list]
     * @Return java.lang.String
     */
    private static String zsetToCopyListKey(String[] keyArray) {
        keyArray[0] = "copy";
        return StringUtils.join(keyArray, ":");
    }

    /**
     * @Description TODO
     * @Date 2020/8/5 下午2:23
     * @Author maoxiaobing
     **/
    private static String zsetToRetryListKey(String[] keyArray) {
        keyArray[0] = "retry";
        return StringUtils.join(keyArray, ":");
    }

    /**
     * @Author maoxiaobing
     * @Description deleteCopy
     * @Date 2020/6/19
     * @Param [key, value]
     * @Return void
     */
    private void deleteCopy(String key, String value) {
        jedisCluster.lrem(key, -1, value);
    }

}
