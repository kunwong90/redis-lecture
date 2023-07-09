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
    public boolean lock(String key, long leaseTime, TimeUnit unit) {
        return tryLock(key, leaseTime, unit);
    }

    @Override
    public boolean unlock(String key) {
        return tryUnlock(key);
    }

    protected String getLockName(long threadId) {
        return id + ":" + threadId;
    }

    public abstract boolean tryLock(String key, long leaseTime, TimeUnit unit);

    public abstract boolean tryUnlock(String key);

    public RedisTemplate<String, String> getRedisTemplate() {
        return redisTemplate;
    }
}
