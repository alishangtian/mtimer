package com.alishangtian.mtimer.client.resultfuture;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @Desc InsertMtimerFuture
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Slf4j
@Builder
public class InsertMtimerFuture implements MtimerFuture {
    private CountDownLatch downLatch = new CountDownLatch(1);

    @Override
    public void waitFinish(Long timeout) throws InterruptedException {
        downLatch.await(timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public void finished() {
        downLatch.countDown();
    }
}
