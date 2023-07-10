package com.redis.lecture.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Component
public class RedisDistributedReentrantLock extends AbstractDistributedLock {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDistributedReentrantLock.class);

    @Override
    public boolean tryLock(String key, long leaseTime, TimeUnit unit) {
        String script =
                // key不存在,直接尝试获取
                "if (redis.call('exists', KEYS[1]) == 0) then " +
                        "redis.call('hincrby', KEYS[1], ARGV[2], 1);" +
                        "redis.call('expire', KEYS[1], ARGV[1]);" +
                        "return true;" +
                        "end;" +
                        // key存在,判断是否是当前线程持有,如果是当前线程持有,计数器加1
                        "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
                        "redis.call('hincrby', KEYS[1], ARGV[2], 1);" +
                        "redis.call('expire', KEYS[1], ARGV[1]);" +
                        "return true;" +
                        "end;" +
                        "return false;";
        try {
            RedisScript<Boolean> redisScript = new DefaultRedisScript<>(script, Boolean.class);
            return getRedisTemplate().execute(redisScript, Collections.singletonList(key), String.valueOf(unit.toSeconds(leaseTime)), getLockName(Thread.currentThread().getId()));
        } catch (Exception e) {
            LOGGER.error("lock failed.", e);
            return true;
        }
    }

    @Override
    public boolean tryUnlock(String key) {
        String script =
                "if (redis.call('hexists', KEYS[1], ARGV[1]) == 0) then " +
                        "return nil;" +
                        "end;" +
                        "local counter = redis.call('hincrby', KEYS[1], ARGV[1], -1);" +
                        "if (counter > 0) then " +
                        "redis.call('expire', KEYS[1], redis.call('ttl', KEYS[1]));" +
                        "return nil;" +
                        "else " +
                        "redis.call('del', KEYS[1]);" +
                        "return nil;" +
                        "end;" +
                        "return nil;";
        try {
            RedisScript<Void> redisScript = new DefaultRedisScript<>(script);
            getRedisTemplate().execute(redisScript, Collections.singletonList(key), getLockName(Thread.currentThread().getId()));
        } catch (Exception e) {
            LOGGER.error("release lock failed.", e);
        }
        return true;
    }

    @Override
    public boolean tryLock(String key, String value, long leaseTime, TimeUnit timeUnit) {
        return tryLock(key, leaseTime, timeUnit);
    }

    @Override
    public boolean tryUnlock(String key, String value) {
        return tryUnlock(key);
    }
}
