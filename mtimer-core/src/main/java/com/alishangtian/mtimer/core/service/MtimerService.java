package com.alishangtian.mtimer.core.service;


import com.alishangtian.mtimer.model.core.MtimerRequest;
import com.alishangtian.mtimer.model.core.MtimerResult;

/**
 * @Desc MtimerService
 * @Time 2020/08/30
 * @Author alishangtian
 */
public interface MtimerService {
    /**
     * @Author maoxiaobing
     * @Description addMtimer
     * @Date 2020/6/19
     * @Param [mtimerRequest]
     * @Return boolean
     */
    MtimerResult addMtimer(MtimerRequest mtimerRequest);

    /**
     * @Author maoxiaobing
     * @Description deleteMtimer
     * @Date 2020/6/19
     * @Param [mtimerRequest]
     * @Return boolean
     */
    Long deleteMtimer(MtimerRequest mtimerRequest);
}
