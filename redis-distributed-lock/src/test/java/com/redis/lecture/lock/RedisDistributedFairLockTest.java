package com.redis.lecture.lock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring/spring-service.xml"})
public class RedisDistributedFairLockTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDistributedFairLockTest.class);

    @Resource
    private RedisDistributedFairLock fairLock;

    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    private static final String LUA_DELETE_LIST = "local list = redis.call('lrange', KEYS[1], 0, -1);" +
            "for index,value in pairs(list) do local prefix = ARGV[1];" +
            "if (string.sub(value, 0, #prefix) ~= prefix) then redis.call('LREM', KEYS[1], 0, value); end end";

    @Test
    public void luaTest() {
        RedisScript<Void> redisScript = new DefaultRedisScript<>(LUA_DELETE_LIST, Void.class);
        redisTemplate.execute(redisScript, Collections.singletonList("distributed_lock_queue:test"), "test");
    }

    @Before
    public void before() {
        redisTemplate.delete("key");
        threadPoolExecutor = new ThreadPoolExecutor(200, 500, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));
    }

    @Test
    public void lockTest() throws InterruptedException {
        IntStream.range(0, 20).forEach(value -> {

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
        });
        threadPoolExecutor.awaitTermination(2, TimeUnit.MINUTES);
    }
}
