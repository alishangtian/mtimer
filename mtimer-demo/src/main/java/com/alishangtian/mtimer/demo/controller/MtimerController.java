package com.alishangtian.mtimer.demo.controller;

import com.alishangtian.mtimer.demo.PackResult;
import com.alishangtian.mtimer.demo.service.MtimerService;
import com.alishangtian.mtimer.model.core.MtimerRequest;
import com.alishangtian.mtimer.model.core.MtimerResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @Desc MtimerController
 * @Time 2020/08/30
 * @Author alishangtian
 */
@RestController
@Slf4j
public class MtimerController {
    @Autowired
    private MtimerService mtimerService;

    @Value("${server.random.origin:5000}")
    private int origin;
    @Value("${server.random.bound:300000}")
    private int bound;

    @PostMapping("/mtimer")
    public Object addMtimer(@RequestBody MtimerRequest mtimerRequest) throws Exception {
        if (null == mtimerRequest.getCallBackTime()) {
            mtimerRequest.setCallBackTime(System.currentTimeMillis() + getRandom());
        }
        if (StringUtils.isBlank(mtimerRequest.getCallBackBody())) {
            mtimerRequest.setCallBackBody(UUID.randomUUID().toString());
        }
        MtimerResult mtimerResult = mtimerService.addMtimer(mtimerRequest);
        if (!mtimerResult.isSuccess()) {
            log.error(mtimerResult.getMsg());
            throw new Exception(mtimerResult.getMsg());
        }
        return PackResult.builder().code(0).msg("success").data(mtimerResult).build();
    }

    @GetMapping("/broker/status")
    public Object brokerStatus() {
        return PackResult.builder().code(1).data(mtimerService.getBrokerStatus()).msg("success").build();
    }

    @PostMapping("/broker/clearredis")
    public Object clearredis() {
        mtimerService.clearRedisData();
        return PackResult.builder().code(1).msg("success").build();
    }

    /**
     * @Description getRandom
     * @Date 2020/8/3 下午3:15
     * @Author maoxiaobing
     **/
    public int getRandom() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return random.nextInt(origin, bound);
    }
}
