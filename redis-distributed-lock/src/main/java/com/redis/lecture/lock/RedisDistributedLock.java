package com.redis.lecture.lock;

import com.redis.lecture.lock.timer.HashedWheelTimer;
import com.redis.lecture.lock.timer.Timeout;
import com.redis.lecture.lock.timer.Timer;
import com.redis.lecture.lock.timer.TimerTask;
import com.redis.lecture.util.NamedThreadFactory;
import com.redis.lecture.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class RedisDistributedLock {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDistributedLock.class);

    private final ThreadLocal<String> threadLocal = new ThreadLocal<>();

    private static final Timer TIMER = new HashedWheelTimer(new NamedThreadFactory("redis-lock-timer", true));

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    private final static Map<String, Timeout> TIMEOUT_MAP = new ConcurrentHashMap<>();


    /**
     * 尝试获取锁，默认持有时间5s，5s后锁自动释放
     * 如果业务执行时间过长,会导致锁自动释放,其他线程可能会重复获取锁
     * 如果获取锁带有等待时间,一个持有锁的进程崩溃,会导致其他进程一直等待直到获得到锁
     *
     * @param key
     * @return
     */
    public boolean tryLock(String key) {
        return tryLock(key, 5, TimeUnit.SECONDS);
    }

    public boolean tryLock(String key, long leaseTime, TimeUnit timeUnit) {
        String nanoTime = String.valueOf(System.nanoTime());
        try {
            RedisScript<String> redisScript = new DefaultRedisScript<>("return redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2], 'NX')", String.class);
            String result = redisTemplate.execute(redisScript, Collections.singletonList(key), nanoTime, String.valueOf(leaseTime));
            if (org.apache.commons.lang3.StringUtils.equalsIgnoreCase(result, "OK")) {
                threadLocal.set(nanoTime);
                Timeout timeout = TIMER.newTimeout(new TimerTask() {
                    @Override
                    public void run(Timeout timeout) throws Exception {
                        List<Object> pipelineResult = redisTemplate.executePipelined((RedisCallback<String>) connection -> {
                            connection.ttl(key.getBytes(), TimeUnit.MICROSECONDS);
                            connection.get(key.getBytes());
                            return null;
                        });
                        Long expire = (Long) pipelineResult.get(0);
                        String value = (String) pipelineResult.get(1);
                        LOGGER.info("expire = {}, value = {}", expire, value);
                        if (expire != null && expire > 0 && StringUtils.isEquals(value, nanoTime)) {
                            Boolean expireResult = redisTemplate.expire(key, leaseTime, timeUnit);
                            LOGGER.info("key:{}, set expire result = {}", key, expireResult);
                            Timeout timeout1 = TIMER.newTimeout(this, leaseTime - 1, timeUnit);
                            TIMEOUT_MAP.put(key, timeout1);
                        } else if (expire != null && expire == -2) {
                            LOGGER.info("key {} doesn't exist", key);
                        } else if (!StringUtils.isEquals(value, nanoTime)) {
                            LOGGER.warn("get redis value not equals.init value = {}, redis value = {}", nanoTime, value);
                        }
                    }
                }, leaseTime - 1, timeUnit);
                TIMEOUT_MAP.put(key, timeout);
            } else {
                return false;
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("tryLock failed.", e);
            return false;
        }
    }

    /**
     * 释放锁
     */
    public void unlock(String key) {
        try {
            String nanoTime = threadLocal.get();
            // 如果业务执行时间过长导致锁自动释放(key时间过期自动删除),当前线程认为自己当前还持有锁
            RedisScript<Boolean> redisScript = new DefaultRedisScript<>("if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Boolean.class);
            redisTemplate.execute(redisScript, Collections.singletonList(key), nanoTime);
        } finally {
            threadLocal.remove();
            Timeout timeout = TIMEOUT_MAP.remove(key);
            if (timeout != null) {
                if (!timeout.isExpired()) {
                    timeout.cancel();
                }
            }
        }
    }
}
