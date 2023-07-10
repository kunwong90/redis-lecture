package com.redis.lecture.lock;

import com.distributed.lock.DistributedLock;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class AbstractDistributedLock implements DistributedLock {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    protected final String id;

    public AbstractDistributedLock() {
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public boolean lock(String key, long leaseTime, TimeUnit timeUnit) {
        return tryLock(key, leaseTime, timeUnit);
    }

    @Override
    public boolean unlock(String key) {
        return tryUnlock(key);
    }

    @Override
    public boolean lock(String key, String value, long leaseTime, TimeUnit timeUnit) {
        return tryLock(key, value, leaseTime, timeUnit);
    }

    @Override
    public boolean unlock(String key, String value) {
        return tryUnlock(key, value);
    }

    protected String getLockName(long threadId) {
        return id + ":" + threadId;
    }

    public abstract boolean tryLock(String key, long leaseTime, TimeUnit timeUnit);

    public abstract boolean tryUnlock(String key);

    public abstract boolean tryLock(String key, String value, long leaseTime, TimeUnit timeUnit);

    public abstract boolean tryUnlock(String key, String value);

    public RedisTemplate<String, String> getRedisTemplate() {
        return redisTemplate;
    }
}
