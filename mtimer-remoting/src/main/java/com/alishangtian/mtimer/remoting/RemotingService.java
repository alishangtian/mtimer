package com.alishangtian.mtimer.remoting;

/**
 * @Desc RemotingService
 * @Time 2020/08/30
 * @Author alishangtian
 */
public interface RemotingService {
    void start();

    void shutdown();

    void registerRPCHook(RPCHook rpcHook);
}
