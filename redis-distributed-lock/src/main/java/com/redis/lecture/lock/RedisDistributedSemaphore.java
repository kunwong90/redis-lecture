package com.redis.lecture.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Component
public class RedisDistributedSemaphore extends AbstractDistributedLock {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDistributedSemaphore.class);

    private static final int MAX_PERMITS = 10;

    @Override
    public boolean tryLock(String key, long leaseTime, TimeUnit unit) {
        String script =
                "local value = redis.call('get', KEYS[1]);" +
                        "if value == false then return redis.call('incr', KEYS[1]);" +
                        "elseif tonumber(value) >= tonumber(ARGV[1]) then return 0;" +
                        "else " +
                        "return redis.call('incr', KEYS[1]);" +
                        "end;";
        try {
            return getRedisTemplate().execute(new DefaultRedisScript<>(script, Long.class),
                    Collections.singletonList(key), String.valueOf(MAX_PERMITS)) > 0;
        } catch (Exception e) {
            LOGGER.error("tryLock error.", e);
            return true;
        }
    }

    @Override
    public boolean tryUnlock(String key) {
        String script =
                "local value = redis.call('get', KEYS[1]);" +
                        "if ((value == false) or tonumber(value) <= 0) then " +
                        "redis.call('del', KEYS[1]); " +
                        "return 0;" +
                        "end;" +
                        "if tonumber(value) > 0 then " +
                        "redis.call('decr', KEYS[1]);" +
                        "return 1;" +
                        "end;";
        try {
            return getRedisTemplate().execute(new DefaultRedisScript<>(script, Long.class),
                    Collections.singletonList(key)) > 0;
        } catch (Exception e) {
            LOGGER.error("tryUnlock error.", e);

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
