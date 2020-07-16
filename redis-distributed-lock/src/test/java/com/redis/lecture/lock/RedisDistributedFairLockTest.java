package com.redis.lecture.lock;

import com.thread.concurrent.util.TraceIdGenerator;
import com.thread.concurrent.util.TracerUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class RedisDistributedFairLockTest extends BaseTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDistributedFairLockTest.class);

    @Resource
    private RedisDistributedFairLock fairLock;

    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private RedisTemplate<String, String> redisTemplate;


    @Before
    public void before() {
        redisTemplate.delete("key");
        threadPoolExecutor = new ThreadPoolExecutor(10, 30, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(100000));
    }

    @Test
    public void lockTest() throws InterruptedException {
        IntStream.range(0, 1).parallel().forEach(value1 -> {
            IntStream.range(0, 30).parallel().forEach(value -> {
                threadPoolExecutor.execute(() -> {
                    String key = "test" + value1;
                    System.out.println(key + ":" + Thread.currentThread().getId() + ",开始执行时间:" + LocalDateTime.now());
                    boolean result = fairLock.lock(key, 2);
                    System.out.println(result);
                    System.out.println(key + " 获取锁成功时间:" + LocalDateTime.now());
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (Exception ignore) {

                    } finally {
                        //fairLock.unlock(key);
                        System.out.println(key + " 释放锁成功时间:" + LocalDateTime.now());
                    }
                });
            });
        });
        threadPoolExecutor.awaitTermination(2, TimeUnit.MINUTES);
    }


    @Test
    public void lockProTest() throws InterruptedException {
        IntStream.range(0, 1).parallel().forEach(value1 -> {
            IntStream.range(0, 50).parallel().forEach(value -> {
                threadPoolExecutor.execute(() -> {
                    MDC.put(TracerUtils.TRACE_ID, TraceIdGenerator.generate());
                    String key = "test" + value1;
                    LOGGER.info(key + ":" + Thread.currentThread().getId() + ",开始执行时间:" + LocalDateTime.now());
                    //System.out.println(key + ":" + Thread.currentThread().getId() + ",开始执行时间:" + LocalDateTime.now());
                    boolean result = fairLock.lockPro(key, 2);
                    //System.out.println(result);
                    if (result) {
                        //System.out.println(key + " 获取锁成功,时间:" + LocalDateTime.now());
                        LOGGER.info(key + " 获取锁成功,时间:" + LocalDateTime.now());
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (Exception ignore) {

                        } finally {
                            //fairLock1.unlock(key);
                            //System.out.println(key + " 释放锁成功时间:" + LocalDateTime.now());
                            LOGGER.info(key + " 释放锁成功时间:" + LocalDateTime.now());
                        }
                    } else {
                        //System.err.println(key + " 获取锁失败,时间:" + LocalDateTime.now());
                        LOGGER.info(key + " 获取锁失败,时间:" + LocalDateTime.now());
                    }
                    MDC.remove(TracerUtils.TRACE_ID);
                });
            });
        });
        threadPoolExecutor.awaitTermination(5, TimeUnit.MINUTES);
    }

    @Test
    public void lock1Test() throws InterruptedException {
        IntStream.range(0, 1).parallel().forEach(value1 -> {
            IntStream.range(0, 100).parallel().forEach(value -> {
                threadPoolExecutor.execute(() -> {
                    System.out.println("value = " + value);
                    MDC.put(TracerUtils.TRACE_ID, TraceIdGenerator.generate());
                    String key = "test" + value1;
                    LOGGER.info(key + ":" + Thread.currentThread().getId() + ",开始执行时间:" + LocalDateTime.now());
                    //System.out.println(key + ":" + Thread.currentThread().getId() + ",开始执行时间:" + LocalDateTime.now());
                    boolean result = fairLock.lock1(key, 2);
                    //System.out.println(result);
                    if (result) {
                        //System.out.println(key + " 获取锁成功,时间:" + LocalDateTime.now());
                        LOGGER.info(key + " 获取锁成功,时间:" + LocalDateTime.now());
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (Exception ignore) {

                        } finally {
                            //fairLock1.unlock(key);
                            //System.out.println(key + " 释放锁成功时间:" + LocalDateTime.now());
                            LOGGER.info(key + " 释放锁成功时间:" + LocalDateTime.now());
                        }
                    } else {
                        //System.err.println(key + " 获取锁失败,时间:" + LocalDateTime.now());
                        LOGGER.info(key + " 获取锁失败,时间:" + LocalDateTime.now());
                    }
                    MDC.remove(TracerUtils.TRACE_ID);
                });
            });
        });
        threadPoolExecutor.awaitTermination(5, TimeUnit.MINUTES);
    }
}
