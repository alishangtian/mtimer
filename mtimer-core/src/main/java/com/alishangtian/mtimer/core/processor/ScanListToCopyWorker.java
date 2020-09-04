package com.alishangtian.mtimer.core.processor;

import com.alishangtian.mtimer.common.util.JSONUtils;
import com.alishangtian.mtimer.model.core.MtimerRequest;
import com.google.common.base.Stopwatch;
import io.netty.util.HashedWheelTimer;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.JedisCluster;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @Desc ScanListToCopyWorker
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Builder
@Slf4j
public class ScanListToCopyWorker implements Runnable {
    private HashedWheelTimer timer;
    private JedisCluster jedisCluster;
    private List<String> keySets;
    private CallBackProcessor callBackProcessor;
    private static final String SCAN_LIST_TO_COPY_SCRIPT = "local result = {}\n" +
            "local page = ARGV[1];\n" +
            "for var=0,page do\n" +
            "    local re = redis.call('RPOPLPUSH',KEYS[1],KEYS[2])\n" +
            "    if(re)\n" +
            "    then\n" +
            "        table.insert(result,re)\n" +
            "    else\n" +
            "        break\n" +
            "    end\n" +
            "end\n" +
            "return result";
    private static final String PAGE_SIZE = "20";
    private static final List<String> ARGS = Arrays.asList(new String[]{PAGE_SIZE});
    private final List<String> keys = Arrays.asList(new String[]{null, null});
    private ExecutorService publicExecutor;
    private ExecutorService triggerExecutor;

    @Override
    public void run() {
        log.info("ScanListToCopyWorker started keysize:{}", keySets.size());
        try {
            keySets.forEach(key -> {
                Stopwatch stopwatch = Stopwatch.createStarted();
                String[] keyArray = StringUtils.split(key, ":");
                String copyKey;
                String listKey;
                String retryKey = zsetToRetryZsetKey(keyArray);
                keys.set(0, (listKey = zsetToListKey(keyArray)));
                keys.set(1, (copyKey = zsetToCopyListKey(keyArray)));
                while (true) {
                    Object result = jedisCluster.eval(SCAN_LIST_TO_COPY_SCRIPT, keys, ARGS);
                    if (null != result && result instanceof List) {
                        List<String> results = (List) result;
                        if (results.size() == 0) {
                            log.info("key {} scan over , cost:{}ms", listKey, stopwatch.elapsed(TimeUnit.MILLISECONDS));
                            break;
                        }
                        try {
                            publicExecutor.submit(() -> {
                                for (String value : results) {
                                    final MtimerRequest mtimerRequest = JSONUtils.parseObject(value, MtimerRequest.class);
                                    long time = System.currentTimeMillis() - mtimerRequest.getCallBackTime();
                                    if (time >= 0) {
                                        log.warn("ScanListToCopyWorker mtimer triggerd delay:{}ms for redis", time);
                                        try {
                                            triggerExecutor.submit(() -> {
                                                try {
                                                    if (callBackProcessor.trigger(mtimerRequest)) {
                                                        deleteCopy(copyKey, value);
                                                    } else {
                                                        jedisCluster.lpush(retryKey, value);
                                                        log.warn("mtimer trigger failed send into retry queue {}", JSONUtils.toJSONString(mtimerRequest));
                                                    }
                                                } catch (Exception e) {
                                                    jedisCluster.lpush(retryKey, value);
                                                    log.error("mtimer trigger error send into retry queue {}", e.getMessage(), e);
                                                }
                                            });
                                        } catch (RejectedExecutionException e) {
                                            jedisCluster.lpush(retryKey, results.toArray(new String[]{}));
                                            log.error("mtimer submit error send into retry queue {}", e.getMessage(), e);
                                        }
                                    } else {
                                        try {
                                            timer.newTimeout(timeout -> {
                                                long delay = System.currentTimeMillis() - mtimerRequest.getCallBackTime();
                                                if (delay > 150) {
                                                    log.warn("ScanListToCopyWorker mtimer triggerd delay:{}ms for hashedwaheeledtimer", System.currentTimeMillis() - mtimerRequest.getCallBackTime());
                                                }
                                                try {
                                                    triggerExecutor.submit(() -> {
                                                        try {
                                                            if (callBackProcessor.trigger(mtimerRequest)) {
                                                                deleteCopy(copyKey, value);
                                                            } else {
                                                                jedisCluster.lpush(retryKey, value);
                                                                log.warn("mtimer trigger failed send into retry queue {}", JSONUtils.toJSONString(mtimerRequest));
                                                            }
                                                        } catch (Exception e) {
                                                            jedisCluster.lpush(retryKey, value);
                                                            log.error("mtimer trigger error send into retry queue {}", e.getMessage(), e);
                                                        }
                                                    });
                                                } catch (RejectedExecutionException e) {
                                                    jedisCluster.lpush(retryKey, value);
                                                    log.error("mtimer trigger submit error send into retry queue {}", e.getMessage(), e);
                                                }
                                            }, mtimerRequest.getCallBackTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                                        } catch (Exception e) {
                                            jedisCluster.lpush(retryKey, value);
                                            log.error("timer newTimeout error send into retry queue {}", e.getMessage(), e);
                                        }
                                    }
                                }
                            });
                        } catch (RejectedExecutionException e) {
                            jedisCluster.lpush(retryKey, results.toArray(new String[]{}));
                            log.error("mtimer submit error send into retry queue {}", e.getMessage(), e);
                        }
                        continue;
                    } else {
                        log.error("unchecked error ,the result {} is not list", JSONUtils.toJSONString(result));
                    }
                    log.info("key {} scan over , cost:{}ms", listKey, stopwatch.elapsed(TimeUnit.MILLISECONDS));
                    break;
                }
            });
        } catch (Exception e) {
            log.error("ScanListToCopyWorker error {}", e.getMessage(), e);
        }
        log.info("ScanListToCopyWorker end");
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
     * @Author maoxiaobing
     * @Description zsetToListKey
     * @Date 2020/6/18
     * @Param [list]
     * @Return java.lang.String
     */
    private static String zsetToListKey(String[] keyArray) {
        keyArray[0] = "list";
        return StringUtils.join(keyArray, ":");
    }

    /**
     * @Description zsetToRetryListKey
     * @Date 2020/8/5 下午2:23
     * @Author maoxiaobing
     **/
    private static String zsetToRetryZsetKey(String[] keyArray) {
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
