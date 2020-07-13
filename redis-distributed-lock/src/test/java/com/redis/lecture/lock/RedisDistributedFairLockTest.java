package com.redis.lecture.lock;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class RedisDistributedFairLockTest extends BaseTest {

    //private static final Logger LOGGER = LoggerFactory.getLogger(RedisDistributedFairLockTest.class);

    @Resource
    private RedisDistributedFairLock fairLock;

    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private RedisTemplate<String, String> redisTemplate;


    @Before
    public void before() {
        redisTemplate.delete("key");
        threadPoolExecutor = new ThreadPoolExecutor(200, 500, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(100000));
    }

    @Test
    public void lockTest() throws InterruptedException {
        IntStream.range(0, 1).parallel().forEach(value1 -> {
            IntStream.range(0, 30).parallel().forEach(value -> {
                threadPoolExecutor.execute(() -> {
                    String key = "test" + value1;
                    System.out.println(key + ":" + Thread.currentThread().getId() + ",开始执行时间:" + Instant.now().toString());
                    boolean result = fairLock.lock(key, 2);
                    System.out.println(result);
                    System.out.println(key + " 获取锁成功时间:" + Instant.now().toString());
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (Exception ignore) {

                    } finally {
                        //fairLock.unlock(key);
                        System.out.println(key + " 释放锁成功时间:" + Instant.now().toString());
                    }
                });
            });
        });
        threadPoolExecutor.awaitTermination(2, TimeUnit.MINUTES);
    }
}
