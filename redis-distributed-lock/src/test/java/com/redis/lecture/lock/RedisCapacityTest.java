package com.redis.lecture.lock;

import org.junit.Test;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.Properties;

public class RedisCapacityTest extends BaseTest {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Test
    public void stringCapacityTest() {
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.flushAll();
            return null;
        });
        Properties properties = redisTemplate.getRequiredConnectionFactory().getConnection().info("memory");
        String before = properties.getProperty("used_memory");
        System.out.println("before = " + before);
        int total = 5698;
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            connection.openPipeline();
            ;
            for (int i = 0; i < total; i++) {
                String key = "key258" + String.format("%16d", i);
                String value = "value7855" + String.format("%19d", i);
                connection.set(key.getBytes(), value.getBytes());
            }
            connection.closePipeline();
            return null;
        });

        properties = redisTemplate.getRequiredConnectionFactory().getConnection().info("memory");
        String after = properties.getProperty("used_memory");
        System.out.println("after = " + after);
        System.out.println("实际占用内存 = " + (Long.parseLong(after) - Long.parseLong(before)));
        System.out.println("理论计算占用内存 = " + cal(total, 16, 19));

    }


    /**
     *  列表对象的底层实现数据结构同样分两种:ziplist或者linkedlist,
     *  当同时满足下面这两个条件时,列表对象使用ziplist这种结构(此处列出的条件都是redis默认配置，可以更改):
     *
     *     列表对象保存的所有字符串元素的长度都小于64字节；
     *
     *     列表对象保存的元素数量小于512个；
     *
     * 因为实际使用情况，这里同样只讲linkedlist结构
     */
    @Test
    public void listCapacityTest() {
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.flushAll();
            return null;
        });
        Properties properties = redisTemplate.getRequiredConnectionFactory().getConnection().info("memory");
        String before = properties.getProperty("used_memory");
        System.out.println("before = " + before);
        int total = 5698;
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            connection.openPipeline();
            ;
            for (int i = 0; i < total; i++) {
                String key = "key258" + String.format("%16d", i);
                String value = "value7855" + String.format("%19d", i);
                connection.set(key.getBytes(), value.getBytes());
            }
            connection.closePipeline();
            return null;
        });

        properties = redisTemplate.getRequiredConnectionFactory().getConnection().info("memory");
        String after = properties.getProperty("used_memory");
        System.out.println("after = " + after);
        System.out.println("实际占用内存 = " + (Long.parseLong(after) - Long.parseLong(before)));
        System.out.println("理论计算占用内存 = " + cal(total, 16, 19));

    }

    static int cal(int num, int key, int value) {
        int keyBytes = tableSizeFor(key + 9);
        int valueBytes = tableSizeFor(value + 9);

        return num * (32 + 16 + keyBytes + valueBytes) + tableSizeFor(num) * 8;
    }


    static final int MAXIMUM_CAPACITY = 1 << 30;

    static int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

}
