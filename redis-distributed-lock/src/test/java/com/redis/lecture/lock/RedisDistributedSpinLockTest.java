package com.redis.lecture.lock;

import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RedisDistributedSpinLockTest extends BaseTest {

    @Resource
    private DistributedLock redisDistributedSpinLock;

    private ThreadPoolExecutor threadPoolExecutor;

    @Before
    public void before() {
        threadPoolExecutor = new ThreadPoolExecutor(100, 100, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));
    }

    @Test
    public void testTryLock() throws InterruptedException {
        String key = "test";
        int count = 10;
        CountDownLatch countDownLatch = new CountDownLatch(count);

        for (int i = 0; i < count; i++) {
            threadPoolExecutor.execute(() -> {
                long start = System.currentTimeMillis();
                boolean result = redisDistributedSpinLock.lock(key, 5, TimeUnit.SECONDS);
                System.out.println(result + " " + (System.currentTimeMillis() - start));
                countDownLatch.countDown();
                System.out.println(redisDistributedSpinLock.unlock(key));
            });
        }
        countDownLatch.await();
    }

    @Test
    public void testTryUnlock() {
    }
}