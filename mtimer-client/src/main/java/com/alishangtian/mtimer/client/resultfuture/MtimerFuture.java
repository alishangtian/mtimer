package com.alishangtian.mtimer.client.resultfuture;

/**
 * @Desc MtimerFuture
 * @Time 2020/08/30
 * @Author alishangtian
 */
public interface MtimerFuture {
    public void waitFinish(Long timeout) throws InterruptedException;

    public void finished();
}
