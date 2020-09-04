package com.alishangtian.mtimer.core.processor;


import com.alishangtian.mtimer.model.core.MtimerRequest;

/**
 * @Desc CallBackProcessor
 * @Time 2020/08/30
 * @Author alishangtian
 */
public interface CallBackProcessor {
    boolean trigger(MtimerRequest mtimerRequest);
}
