package com.redis.sentinel.lecture;

import org.junit.Test;

import javax.annotation.Resource;

public class StringRedisOperationTest extends BaseTest {

    @Resource
    private StringRedisOperation stringRedisOperation;

    @Test
    public void setTest() {
        stringRedisOperation.set("testKey", "testValue");
    }
}