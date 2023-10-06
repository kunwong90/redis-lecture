package com.redis.lecture.circuitbreake;

import com.redis.lecture.lock.BaseTest;
import org.junit.Test;

import javax.annotation.Resource;

public class RedisCommandsWithCircuitBreakerTest extends BaseTest {

    @Resource(name = "redisCommandsWithCircuitBreaker")
    private RedisCommands redisCommands;

    private String key = "test";
    private String value = "value";

    private String hashKey = "hashtest";

    @Test
    public void set() {
        System.out.println(redisCommands.set(key, value));
    }

    @Test
    public void get() {
        System.out.println(redisCommands.get(key));
    }

    @Test
    public void hstrlen() {
        System.out.println(redisCommands.hstrlen(hashKey, "2"));
    }

    @Test
    public void move() {
        System.out.println(redisCommands.move(key, 0));
    }
}