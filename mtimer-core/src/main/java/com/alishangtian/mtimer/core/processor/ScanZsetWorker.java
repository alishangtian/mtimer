package com.alishangtian.mtimer.core.processor;

import com.google.common.base.Stopwatch;
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
 * @Desc ScanZsetWorker
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Builder
@Slf4j
public class ScanZsetWorker implements Runnable {
    private JedisCluster jedisCluster;
    private List<String> keySets;
    private ExecutorService executorService;
    private static final String SCAN_ZSET_SCRIPT =
            "local result = redis.call('ZRANGEBYSCORE',KEYS[1],ARGV[1],ARGV[2],'LIMIT', ARGV[3] ,ARGV[4])\n" +
                    "for k, v in pairs(result) do\n" +
                    "    redis.call('ZREM',KEYS[1],v)\n" +
                    "    redis.call('LPUSH',KEYS[2],v)\n" +
                    "end\n" +
                    "return #result";
    private static final String OFFSET = "0";
    private static final String SIZE = "20";
    private static final Long TEN_MINUTES_OF_MILLIS = 600000L;
    private final List<String> args = Arrays.asList(new String[]{null, null, OFFSET, SIZE});

    @Override
    public void run() {
        log.info("ScanZsetWorker start keysize:{}", keySets.size());
        try {
            Long now = System.currentTimeMillis();
            Long start = now - TEN_MINUTES_OF_MILLIS;
            Long end = now + TEN_MINUTES_OF_MILLIS;
            args.set(0, start.toString());
            args.set(1, end.toString());
            keySets.forEach(key -> {
                try {
                    executorService.submit(() -> {
                        try {
                            Stopwatch stopwatch = Stopwatch.createStarted();
                            long count = 0L;
                            long result;
                            do {
                                String listKey = zsetToListKey(key);
                                List<String> keys = Arrays.asList(new String[]{key, listKey});
                                result = (long) jedisCluster.eval(SCAN_ZSET_SCRIPT, keys, args);
                                count += result;
                            } while (result > 0);
                            log.info("key {} scan over cost:{}ms , mtimer count:{}", key, stopwatch.elapsed(TimeUnit.MILLISECONDS), count);
                        } catch (Exception e) {
                            log.error("key {} scan error {}", key, e.getMessage(), e);
                        }
                    });
                } catch (RejectedExecutionException e) {
                    log.error("ScanZsetWorker executor submit error {}", e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            log.error("ScanZsetWorker run error {}", e.getMessage(), e);
        }
        log.info("ScanZsetWorker end size:{}", keySets.size());
    }

    /**
     * @Author maoxiaobing
     * @Description zsetToListKey
     * @Date 2020/6/18
     * @Param [zset]
     * @Return java.lang.String
     */
    private String zsetToListKey(String zset) {
        String[] zsetKey = StringUtils.split(zset, ":");
        zsetKey[0] = "list";
        return StringUtils.join(zsetKey, ":");
    }
}
