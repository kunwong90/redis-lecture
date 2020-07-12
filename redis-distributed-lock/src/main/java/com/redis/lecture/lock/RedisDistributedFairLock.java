package com.redis.lecture.lock;

import com.redis.lecture.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * redis实现的分布式公平锁
 */
@Component
public class RedisDistributedFairLock {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDistributedFairLock.class);

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    private ThreadLocal<String> threadLocal = new ThreadLocal<>();

    private static final String PREFIX_LIST_QUEUE_NAME = "distributed_lock_queue:";

    /**
     * 每次启动生成的一个标识
     */
    private final UUID uuid;

    private RedisDistributedFairLock() {
        this.uuid = UUID.randomUUID();
    }

    private static final String LUA_DELETE_LIST =
            "local list = redis.call('lrange', KEYS[1], 0, -1);" +
            "for index,value in pairs(list) do local prefix = ARGV[1];" +
                "if (string.sub(value, 0, #prefix) ~= prefix) then " +
                    "redis.call('LREM', KEYS[1], 0, value); " +
                "end;" +
            "end;" +
            "redis.call('rpush', KEYS[1], ARGV[2]);";


    public boolean lock(String key, long time, TimeUnit timeUnit) {
        String listKey = PREFIX_LIST_QUEUE_NAME + key;
        String identifier = uuid.toString();
        try {
            String threadId = String.valueOf(Thread.currentThread().getId());
            String value = identifier + ":" + key + ":" + threadId + ":" + UUID.randomUUID().toString();
            RedisScript<Void> redisScript = new DefaultRedisScript<>(LUA_DELETE_LIST, Void.class);
            redisTemplate.execute(redisScript, Collections.singletonList(listKey), identifier, value);
            //redisTemplate.opsForList().rightPush(listKey, value);
            while (true) {
                String result = redisTemplate.opsForList().index(listKey, 0);
                //LOGGER.info("pushValue = {}, popValue = {}", value, result);
                if (StringUtils.isEquals(value, result)) {
                    Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, time, timeUnit);
                    if (success != null && success) {
                        threadLocal.set(value);
                        //LOGGER.info("==自旋获取锁成功== " + value);
                        redisTemplate.opsForList().leftPop(listKey);
                        // 获取锁成功
                        return true;
                    }
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (Exception ignore) {

                }
            }
        } catch (Exception e) {
            LOGGER.error("lock error.", e);
            return false;
        }
    }

    /**
     * 释放锁
     */
    public void unlock(String key) {
        try {
            String threadId = threadLocal.get();
            LOGGER.info("==释放锁==" + threadId);
            // 如果业务执行时间过长导致锁自动释放(key时间过期自动删除),当前线程认为自己当前还持有锁
            RedisScript<Boolean> redisScript = new DefaultRedisScript<>("if redis.call('get', KEYS[1]) == KEYS[2] then return redis.call('del', KEYS[1]) else return 0 end", Boolean.class);
            redisTemplate.execute(redisScript, Arrays.asList(key, threadId));
        } finally {
            threadLocal.remove();
        }
    }


    public boolean lock(String key, long time) {
        return lock(key, time, TimeUnit.SECONDS);
    }

    public Set<String> scan(String matchKey) {
        return redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keysTmp = new HashSet<>();
            Cursor<byte[]> cursor = connection.scan(new ScanOptions.ScanOptionsBuilder().match(matchKey + "*").count(1000).build());
            while (cursor.hasNext()) {
                keysTmp.add(new String(cursor.next()));
            }
            return keysTmp;
        });
    }
}
