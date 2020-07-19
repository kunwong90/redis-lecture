package com.redis.lecture.lock;

import org.junit.Test;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

public class RedisDistributedSemaphoreTest extends BaseTest {

    @Resource
    private RedisDistributedSemaphore redisDistributedSemaphore;

    @Test
    public void tryLock() {
        for (int i = 0; i < 10; i++) {
            System.out.println(redisDistributedSemaphore.tryLock("test", 1, TimeUnit.SECONDS));
        }
    }

    @Test
    public void tryUnlock() {
        for (int i = 0; i < 10; i++) {
            System.out.println(redisDistributedSemaphore.tryUnlock("test"));
        }
    }
}
