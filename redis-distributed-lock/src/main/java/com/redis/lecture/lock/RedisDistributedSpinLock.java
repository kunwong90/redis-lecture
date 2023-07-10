package com.redis.lecture.lock;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Component
public class RedisDistributedSpinLock extends AbstractDistributedLock {

    private long lockLeaseTime = 10L;

    @Override
    public boolean tryLock(String key, long leaseTime, TimeUnit unit) {

        Long ttl = getTtl(key, leaseTime, unit);
        if (ttl == null) {
            return true;
        }
        while (ttl != null) {
            long nextSleepPeriod = LockOptions.defaults().create().getNextSleepPeriod();
            try {
                Thread.sleep(nextSleepPeriod);
            } catch (InterruptedException e) {

            }
            ttl = getTtl(key, leaseTime, unit);
        }


        return true;
    }

    @Override
    public boolean tryUnlock(String key) {

        RedisScript<Long> redisScript = new DefaultRedisScript<>("if (redis.call('hexists', KEYS[1], ARGV[2]) == 0) then " +
                "return nil;" +
                "end; " +
                "local counter = redis.call('hincrby', KEYS[1], ARGV[2], -1); " +
                "if (counter > 0) then " +
                "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                "return 0; " +
                "else " +
                "redis.call('del', KEYS[1]); " +
                "return 1; " +
                "end; " +
                "return nil;", Long.class);
        Long result = getRedisTemplate().execute(redisScript, Collections.singletonList(key), String.valueOf(lockLeaseTime), getLockName(Thread.currentThread().getId()));
        return result == null || result > 0;
    }

    @Override
    public boolean tryLock(String key, String value, long leaseTime, TimeUnit timeUnit) {
        return tryLock(key, leaseTime, timeUnit);
    }

    @Override
    public boolean tryUnlock(String key, String value) {
        return tryUnlock(key);
    }

    private Long getTtl(String key, long leaseTime, TimeUnit unit) {
        this.lockLeaseTime = unit.toMillis(leaseTime);
        RedisScript<Long> redisScript = new DefaultRedisScript<>("if (redis.call('exists', KEYS[1]) == 0) then " +
                "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
                "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                "return nil; " +
                "end; " +
                "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
                "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
                "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                "return nil; " +
                "end; " +
                "return redis.call('pttl', KEYS[1]);", Long.class);
        return getRedisTemplate().execute(redisScript, Collections.singletonList(key), String.valueOf(lockLeaseTime), getLockName(Thread.currentThread().getId()));


    }
}
