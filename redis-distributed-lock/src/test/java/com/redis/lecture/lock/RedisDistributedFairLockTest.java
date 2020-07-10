package com.redis.lecture.lock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring/spring-service.xml"})
public class RedisDistributedFairLockTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDistributedFairLockTest.class);

    @Resource
    private RedisDistributedFairLock fairLock;

    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Before
    public void before() {
        redisTemplate.delete("key");
        threadPoolExecutor = new ThreadPoolExecutor(200, 500, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));
    }

    @Test
    public void lockTest() throws InterruptedException {

        for (int i = 0; i < 10; i++) {
            threadPoolExecutor.execute(() -> {
                System.out.println(Thread.currentThread().getId() + ",开始执行时间:" + Instant.now().toString());
                String key = "test";
                boolean result = fairLock.lock(key, 2, TimeUnit.SECONDS);
                //LOGGER.info("result = {}", result);
                System.out.println(result);
                System.out.println("获取锁成功时间:" + Instant.now().toString());
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (Exception e) {

                } finally {
                    fairLock.unlock(key);
                    System.out.println("释放锁成功时间:" + Instant.now().toString());
                    //LOGGER.info("release lock success.time cost = {}", (System.currentTimeMillis() - start));
                }
            });
        }
        threadPoolExecutor.awaitTermination(1,  TimeUnit.MINUTES);
    }
}
