package com.redis.lecture.lock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring/spring-service.xml"})
public class RedisDistributedFairLockTest {

    @Resource
    private RedisDistributedFairLock fairLock;

    private ThreadPoolExecutor threadPoolExecutor;

    @Before
    public void before() {
        threadPoolExecutor = new ThreadPoolExecutor(100, 200, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));
    }

    @Test
    public void lockTest() throws InterruptedException {

        IntStream.range(0, 25).forEach(value -> {
            threadPoolExecutor.execute(() -> {
                long start = System.currentTimeMillis();
                String key = "test";
                boolean result = fairLock.lock(key, 2, TimeUnit.SECONDS);
                System.out.println(result);
                /*try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception e) {

                } finally {
                    fairLock.unlock(key);
                }*/
                System.out.println("time cost = " + (System.currentTimeMillis() - start));
            });
        });

        IntStream.range(0, 40).forEach(value -> {
            threadPoolExecutor.execute(() -> {
                long start = System.currentTimeMillis();
                String key = "test1";
                boolean result = fairLock.lock(key, 10, TimeUnit.SECONDS);
                System.out.println(result);
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception e) {

                } finally {
                    fairLock.unlock(key);
                }
                System.out.println("time cost = " + (System.currentTimeMillis() - start));
            });
        });

        IntStream.range(0, 25).forEach(value -> {
            threadPoolExecutor.execute(() -> {
                long start = System.currentTimeMillis();
                String key = "test2";
                boolean result = fairLock.lock(key, 10, TimeUnit.SECONDS);
                System.out.println(result);
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception e) {

                } finally {
                    fairLock.unlock(key);
                }
                System.out.println("time cost = " + (System.currentTimeMillis() - start));
            });
        });
        IntStream.range(0, 30).forEach(value -> {
            threadPoolExecutor.execute(() -> {
                long start = System.currentTimeMillis();
                String key = "test3";
                boolean result = fairLock.lock(key, 10, TimeUnit.SECONDS);
                System.out.println(result);
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception e) {

                } finally {
                    fairLock.unlock(key);
                }
                System.out.println("time cost = " + (System.currentTimeMillis() - start));
            });
        });


        //threadPoolExecutor.awaitTermination(50, TimeUnit.SECONDS);
    }
}
