package com.redis.lecture.ratelimiter;

import com.redis.lecture.lock.BaseTest;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TokenBucketRateLimiterTest extends BaseTest {


    @Resource
    private RateLimiter rateLimiter;

    @Test
    public void testRateLimit() throws InterruptedException {
        String key = "test_rateLimit_key";
        int max = 10;  //令牌桶大小
        int rate = 10; //令牌每秒恢复速度
        int size = 1;
        AtomicInteger successCount = new AtomicInteger(0);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(30));
        CountDownLatch countDownLatch = new CountDownLatch(size);
        for (int i = 0; i < size; i++) {
            executor.execute(() -> {
                try {
                    boolean isAllow = rateLimiter.rateLimit(key, max, rate);
                    if (isAllow) {
                        successCount.addAndGet(1);
                    }
                    logger.info(Boolean.toString(isAllow));

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
        logger.info("请求成功{}次", successCount.get());
        executor.shutdown();
    }
}