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
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
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

    /**
     * KEYS[1] list的key
     * KEYS[2] 是传入的key
     * ARGV[1] 是应用启动生成的标识
     * ARGV[2] 是 KEYS[2]对应的value
     * ARGV[3] 是KEYS[2]的过期时间
     */
    private static final String LUA_DELETE_LIST =
            "local list = redis.call('lrange', KEYS[1], 0, -1);" +
            "for index,value in pairs(list) do " +
                    "if (string.sub(value, 0, #ARGV[1]) ~= ARGV[1]) then " +
                        "redis.call('LREM', KEYS[1], 0, value);" +
                    "end;" +
            "end;" +
            "redis.call('rpush', KEYS[1], ARGV[2]);" +
            "while true do " +
                "local firstValue = redis.call('lindex', KEYS[1], 0);" +
                "if firstValue == ARGV[2] then " +
                    "local result = redis.call('setnx', KEYS[2], ARGV[2]);" +
                        "if result == 1 then " +
                            "redis.call('expire', KEYS[2], ARGV[3]);" +
                            "redis.call('lpop', KEYS[1]);" +
                            "break;" +
                        "end;" +
                "end;" +
            "end;";

    @Test
    public void luaTest() {
        RedisScript<Void> redisScript = new DefaultRedisScript<>(LUA_DELETE_LIST, Void.class);
        redisTemplate.execute(redisScript, Collections.singletonList("distributed_lock_queue:test"), "test");
    }


    @Test
    public void luaTest1() {
        RedisScript<Void> redisScript = new DefaultRedisScript<>(LUA_DELETE_LIST, Void.class);
        String key = "test";
        String argv1 = UUID.randomUUID().toString();
        String argv2 = argv1 + ":" + key + ":" + Thread.currentThread().getId() + ":" + UUID.randomUUID().toString();
        String argv3 = String.valueOf(100);
        redisTemplate.execute(redisScript, Arrays.asList("distributed_lock_queue:" + key, key), argv1, argv2, argv3);
    }

    @Test
    public void luaTest3() {
        String lua = "local result = 1;" +
                "while true do " +
                    "local firstThreadId2 = redis.call('lindex', KEYS[1], 0);" +
                    "if firstThreadId2 == false then " +
                        "result = 0; break;" +
                    "else " +
                        "result = 2; break;" +
                    "end;" +
                "end; return result;";
        RedisScript<Long> redisScript = new DefaultRedisScript<>(lua, Long.class);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList("test"));
        System.out.println(result);
    }

    @Test
    public void luaTest4() {
        String lua = "local result = redis.call('setnx', KEYS[1], ARGV[1]);" +
                "redis.log(redis.LOG_NOTICE, result);" +
                "if result == 1 then " +
                "redis.log(redis.LOG_NOTICE, 'set success')" +
                "redis.call('expire', KEYS[1], 10) end;";
        RedisScript<Void> redisScript = new DefaultRedisScript<>(lua, Void.class);
        redisTemplate.execute(redisScript, Collections.singletonList("test"), UUID.randomUUID().toString());
    }

    @Before
    public void before() {
        redisTemplate.delete("key");
        threadPoolExecutor = new ThreadPoolExecutor(200, 500, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(100000));
    }

    @Test
    public void lockTest() throws InterruptedException {
        IntStream.range(0, 10).forEach(value -> {

            threadPoolExecutor.execute(() -> {
                System.out.println(Thread.currentThread().getId() + ",开始执行时间:" + Instant.now().toString());
                String key = "test";
                boolean result = fairLock.lock(key, 2, TimeUnit.SECONDS);
                //LOGGER.info("result = {}", result);
                System.out.println(result);
                System.out.println("获取锁成功时间:" + Instant.now().toString());
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception e) {

                } finally {
                    fairLock.unlock(key);
                    System.out.println("释放锁成功时间:" + Instant.now().toString());
                    //LOGGER.info("release lock success.time cost = {}", (System.currentTimeMillis() - start));
                }
            });
        });
        threadPoolExecutor.awaitTermination(200, TimeUnit.MINUTES);
    }

    @Test
    public void lockWithLua() throws InterruptedException {
        IntStream.range(0, 2).forEach(value -> {

            threadPoolExecutor.execute(() -> {
                System.out.println(Thread.currentThread().getId() + ",开始执行时间:" + Instant.now().toString());
                String key = "test";
                boolean result = fairLock.lockWithLua(key, 2, TimeUnit.SECONDS);
                System.out.println(result);
                System.out.println("获取锁成功时间:" + Instant.now().toString());
                //fairLock.unlock(key);
                //System.out.println("释放锁成功时间:" + Instant.now().toString());
            });
        });
        threadPoolExecutor.awaitTermination(1, TimeUnit.MINUTES);
    }

    @Test
    public void test4() {

        /**
         * "local exists = redis.call('exists', KEYS[1]);" +
         *  存在返回1，不存在返回0
         */

        // set 失败返回false
        String lua = "local result = redis.call('set', KEYS[1], ARGV[1], 'ex', ARGV[2], 'nx');" +
                "if result ~= false then " +
                    "return 1;" +
                "else " +
                    "return 0;" +
                "end;";

        /**
         * 成功返回1，失败返回0
         */
        /*String lua = "local result = redis.call('setnx', KEYS[1], ARGV[1]) " +
                "if result == 1 then " +
                "return 1;" +
                "else " +
                "return 0;" +
                "end;";*/
        RedisScript<Long> redisScript = new DefaultRedisScript<>(lua, Long.class);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList("test"), UUID.randomUUID().toString(), String.valueOf(12));
        System.out.println(result);
    }
}
