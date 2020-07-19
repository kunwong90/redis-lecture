package com.redis.lecture.lock;

import com.redis.lecture.util.NamedThreadFactory;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RedisDistributedReentrantLockTest extends BaseTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDistributedReentrantLock.class);

    @Resource
    private RedisDistributedReentrantLock redisDistributedReentrantLock;
    private ThreadPoolExecutor threadPoolExecutor;

    @Before
    public void before() {
        threadPoolExecutor = new ThreadPoolExecutor(10, 20,
                1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(100),
                new NamedThreadFactory("redis-reentrant-lock"));
    }

    String key = "REDIS:DISTRIBUTE:LOCK:TEST";
    @Test
    public void tryLock() {

        boolean result = redisDistributedReentrantLock.tryLock(key, 1, TimeUnit.MINUTES);
        LOGGER.info("线程 {} 获取锁 {}", Thread.currentThread().getId(), result ? "成功" : "失败");
        result = redisDistributedReentrantLock.tryLock(key, 1, TimeUnit.MINUTES);
        LOGGER.info("线程 {} 获取锁 {}", Thread.currentThread().getId(), result ? "成功" : "失败");
        result = redisDistributedReentrantLock.tryLock(key, 1, TimeUnit.MINUTES);
        LOGGER.info("线程 {} 获取锁 {}", Thread.currentThread().getId(), result ? "成功" : "失败");
        redisDistributedReentrantLock.tryUnlock(key);
        redisDistributedReentrantLock.tryUnlock(key);
        redisDistributedReentrantLock.tryUnlock(key);

        threadPoolExecutor.execute(() -> {
            boolean result1 = redisDistributedReentrantLock.tryLock(key, 1, TimeUnit.MINUTES);
            LOGGER.info("线程 {} 获取锁 {}", Thread.currentThread().getId(), result1 ? "成功" : "失败");
        });
    }

    @Test
    public void tryUnlock() {


    }
}
