package com.alishangtian.redis;

import redis.clients.jedis.util.JedisClusterCRC16;

import java.util.Arrays;

/**
 * @Description Crc16
 * @Date 2020/6/18 下午3:17
 * @Author maoxiaobing
 **/
public class Crc16 {
    public static void main(String[] args) {
        Arrays.asList(new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"})
                .forEach(s -> System.out.println(String.format("value:%s,slot:%s", s, JedisClusterCRC16.getSlot(s))));
    }
}
