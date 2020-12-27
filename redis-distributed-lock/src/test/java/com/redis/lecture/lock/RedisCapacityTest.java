package com.redis.lecture.lock;

import org.junit.Test;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class RedisCapacityTest extends BaseTest {

    /**
     * redisObject对象占16字节
     */
    static final int REDIS_OBJECT_SIZE = 16;

    /**
     * redis dictEntry对象占24字节,jemalloc会分配32字节的内存块
     */
    static final int REDIS_DICT_ENTRY_SIZE = 24;

    /**
     * redis 指针大小占8字节
     */
    static final int REDIS_POINTER_SIZE = 8;

    static final int REDIS_LIST_NODE_SIZE = 24;

    static final int REDIS_LIST_SIZE = 48;

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
        int keyNum = 2000;
        AtomicInteger keyLength = new AtomicInteger();
        AtomicInteger valueLength = new AtomicInteger();
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            connection.openPipeline();
            for (int i = 0; i < keyNum; i++) {
                String key = "test_key_" + String.format("%04d", i);
                keyLength.set(key.getBytes().length);
                String value = "test_value_" + String.format("%06d", i);
                valueLength.set(value.getBytes().length);
                connection.set(key.getBytes(), value.getBytes());
            }
            connection.closePipeline();
            return null;
        });

        properties = redisTemplate.getRequiredConnectionFactory().getConnection().info("memory");
        String after = properties.getProperty("used_memory");
        System.out.println("after = " + after);
        long actual = (Long.parseLong(after) - Long.parseLong(before));
        System.out.println("实际占用内存 = " + actual);
        int cal = calStringSize(keyNum, keyLength.get(), valueLength.get());
        System.out.println("理论计算占用内存 = " + cal);
        System.out.println("实际占用内存是否等于理论值:" + (cal == actual));

    }

    static int calStringSize(int num, int key, int value) {
        return num * (tableSizeFor(REDIS_DICT_ENTRY_SIZE) +
                tableSizeFor(REDIS_OBJECT_SIZE) +
                tableSizeFor(key + 3) +
                tableSizeFor(value + 3)) + tableSizeFor(num) * REDIS_POINTER_SIZE;
    }

    /**
     * 列表对象的底层实现数据结构同样分两种:ziplist或者linkedlist,
     * 当同时满足下面这两个条件时,列表对象使用ziplist这种结构(此处列出的条件都是redis默认配置，可以更改):
     * <p>
     * 列表对象保存的所有字符串元素的长度都小于64字节；
     * <p>
     * 列表对象保存的元素数量小于512个；
     * <p>
     * 因为实际使用情况，这里同样只讲linkedlist结构
     */
    @Test
    public void listLinkedListCapacityTest() {
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.flushAll();
            return null;
        });
        Properties properties = redisTemplate.getRequiredConnectionFactory().getConnection().info("memory");
        String before = properties.getProperty("used_memory");
        System.out.println("before = " + before);
        int keyNum = 2;
        int listSize = 20;

        for (int i = 0; i < keyNum; i++) {
            String key = "key" + String.format("%06d", i);
            for (int j = 0; j < listSize; j++) {
                String value = "value" + String.format("%010d", j);
                redisTemplate.boundListOps(key).rightPush(value);
            }
        }

        properties = redisTemplate.getRequiredConnectionFactory().getConnection().info("memory");
        String after = properties.getProperty("used_memory");
        System.out.println("after = " + after);
        long actual = (Long.parseLong(after) - Long.parseLong(before));
        System.out.println("实际占用内存 = " + actual);
        int cal = calLinkedListSize(keyNum, 9, 15, listSize);
        System.out.println("理论计算占用内存 = " + cal);
        System.out.println("实际占用内存是否等于理论值:" + (cal == actual));

    }


    static int calLinkedListSize(int keyNum, int keyByteLength, int valueByteLength, int listSize) {
        int keyLength = (keyByteLength + 3);
        int valueLength = (valueByteLength + 3);
        return ((valueLength + tableSizeFor(REDIS_OBJECT_SIZE) + tableSizeFor(REDIS_LIST_NODE_SIZE)) * listSize + REDIS_LIST_SIZE + tableSizeFor(REDIS_OBJECT_SIZE) + keyLength + tableSizeFor(REDIS_DICT_ENTRY_SIZE)) * keyNum + tableSizeFor(keyNum) * 8;
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


    @Test
    public void hashHashTableCapacityTest() {
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.flushAll();
            return null;
        });
        Properties properties = redisTemplate.getRequiredConnectionFactory().getConnection().info("memory");
        String before = properties.getProperty("used_memory");
        System.out.println("before = " + before);
        int keyNum = 200;
        int fieldNum = 200;

        String valuePrefix = "test_value_123456789012345678901234567890123456789012345678901234567890_";
        for (int i = 0; i < keyNum; i++) {
            String key = "key" + String.format("%09d", i);
            for (int j = 0; j < fieldNum; j++) {
                String field = "field" + String.format("%09d", j);
                String value = valuePrefix + String.format("%03d", j);
                redisTemplate.boundHashOps(key).put(field, value);
            }
        }

        properties = redisTemplate.getRequiredConnectionFactory().getConnection().info("memory");
        String after = properties.getProperty("used_memory");
        System.out.println("after = " + after);
        long actual = (Long.parseLong(after) - Long.parseLong(before));
        System.out.println("实际占用内存 = " + actual);
        int cal = calHashHashTableSize(16, 14, 75, 24, 200, 8, 88, 12, 200);
        System.out.println("理论计算占用内存 = " + cal);
        System.out.println("实际占用内存是否等于理论值:" + (cal == actual));

    }

    /**
     * 计算哈希使用hashtable时的容量
     *
     * @return
     */
    static int calHashHashTableSize(int redisObjectSize, int fieldSdsSize, int valueSdsSize, int dictEntrySize,
                                    int fieldNum, int pointSize, int dictSize, int keySdsSize, int keyNum) {


        return ((redisObjectSize * 2 + tableSizeFor(fieldSdsSize + 9) + tableSizeFor(valueSdsSize) + tableSizeFor(dictEntrySize)) * fieldNum
                + tableSizeFor(fieldNum) * pointSize + dictSize + redisObjectSize + keySdsSize + dictEntrySize
        ) * keyNum + tableSizeFor(keyNum) * pointSize;
    }
}
