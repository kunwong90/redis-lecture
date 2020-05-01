package com.redis.lecture.lock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring/spring-service.xml"})
public class RedisDistributedLockTest {

    static final String KEY = "REDIS:DISTRIBUTE:LOCK:TEST";

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDistributedLockTest.class);
    @Resource
    private RedisDistributedLock redisDistributedLock;

    private ThreadPoolExecutor threadPoolExecutor;

    @Before
    public void before() {
        threadPoolExecutor = new ThreadPoolExecutor(100, 100, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));
    }

    @Test
    public void tryLock() {
        redisDistributedLock.tryLock(KEY, 5, TimeUnit.SECONDS);

        for (int i = 0; i < 500; i++) {
            threadPoolExecutor.execute(() -> {
                IntStream.range(0, 10).forEach(j -> {
                    boolean result = redisDistributedLock.tryLock(KEY, 3, TimeUnit.SECONDS);
                    if (result) {
                        LOGGER.info("线程 {} 获取锁成功", Thread.currentThread().getName());
                    } else {
                        LOGGER.info("Thread {} get lock failed.", new long[]{Thread.currentThread().getId()});
                    }
                });
            });
        }

        try {
            TimeUnit.MINUTES.sleep(10);
        } catch (Exception ignore) {

        }
        redisDistributedLock.unlock(KEY);

    }

    @Test
    public void reentrantLockTest() {
        boolean result = redisDistributedLock.reentrantLock(KEY, 1, TimeUnit.MINUTES);
        LOGGER.info("线程 {} 获取锁 {}", Thread.currentThread().getId(), result ? "成功" : "失败");
        result = redisDistributedLock.reentrantLock(KEY, 1, TimeUnit.MINUTES);
        LOGGER.info("线程 {} 获取锁 {}", Thread.currentThread().getId(), result ? "成功" : "失败");
        result = redisDistributedLock.reentrantLock(KEY, 1, TimeUnit.MINUTES);
        LOGGER.info("线程 {} 获取锁 {}", Thread.currentThread().getId(), result ? "成功" : "失败");
        redisDistributedLock.reentrantUnlock(KEY);
        redisDistributedLock.reentrantUnlock(KEY);

        threadPoolExecutor.execute(() -> {
            boolean result1 = redisDistributedLock.reentrantLock(KEY, 1, TimeUnit.MINUTES);
            LOGGER.info("线程 {} 获取锁 {}", Thread.currentThread().getId(), result1 ? "成功" : "失败");
        });
    }
}
