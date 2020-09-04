package com.alishangtian.mtimer.core.processor;

import com.alishangtian.mtimer.model.core.MtimerRequest;

/**
 * @Desc InvokerMtimerProcessor
 * @Time 2020/08/30
 * @Author alishangtian
 */
public interface InvokerMtimerProcessor {
    /**
     * @Description
     * @Date 2020/6/23 下午4:40
     * @Author maoxiaobing
     **/
    boolean invokeMtimerTrigger(MtimerRequest mtimerRequest);
}
