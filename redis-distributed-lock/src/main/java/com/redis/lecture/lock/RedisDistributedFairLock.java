package com.redis.lecture.lock;

import com.redis.lecture.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * redis实现的分布式公平锁
 */
@Component
public class RedisDistributedFairLock {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDistributedFairLock.class);

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 单位毫秒
     */
    private final long threadWaitTime = 5000;

    @Resource
    private RedisDistributedLock redisDistributedLock;

    private static final String LUA_SCRIPT = "while true do " +
            "local firstThreadId2 = redis.call('lindex', KEYS[2], 0);" +
            "if firstThreadId2 == false then " +
            "break;" +
            "end;" +

            "local timeout = tonumber(redis.call('zscore', KEYS[3], firstThreadId2));" +
            "if timeout <= tonumber(ARGV[4]) then " +
            // remove the item from the queue and timeout set
            // NOTE we do not alter any other timeout
            "redis.call('zrem', KEYS[3], firstThreadId2);" +
            "redis.call('lpop', KEYS[2]);" +
            "else " +
            "break;" +
            "end;" +
            "end;" +

            // check if the lock can be acquired now
            "if (redis.call('exists', KEYS[1]) == 0) " +
            "and ((redis.call('exists', KEYS[2]) == 0) " +
            "or (redis.call('lindex', KEYS[2], 0) == ARGV[2])) then " +

            // remove this thread from the queue and timeout set
            "redis.call('lpop', KEYS[2]);" +
            "redis.call('zrem', KEYS[3], ARGV[2]);" +

            // decrease timeouts for all waiting in the queue
            "local keys = redis.call('zrange', KEYS[3], 0, -1);" +
            "for i = 1, #keys, 1 do " +
            "redis.call('zincrby', KEYS[3], -tonumber(ARGV[3]), keys[i]);" +
            "end;" +

            // acquire the lock and set the TTL for the lease
            "redis.call('hset', KEYS[1], ARGV[2], 1);" +
            "redis.call('pexpire', KEYS[1], ARGV[1]);" +
            "return nil;" +
            "end;" +

            // check if the lock is already held, and this is a re-entry
            "if redis.call('hexists', KEYS[1], ARGV[2]) == 1 then " +
            "redis.call('hincrby', KEYS[1], ARGV[2],1);" +
            "redis.call('pexpire', KEYS[1], ARGV[1]);" +
            "return nil;" +
            "end;" +

            // the lock cannot be acquired
            // check if the thread is already in the queue
            "local timeout = redis.call('zscore', KEYS[3], ARGV[2]);" +
            "if timeout ~= false then " +
            // the real timeout is the timeout of the prior thread
            // in the queue, but this is approximately correct, and
            // avoids having to traverse the queue
            "return timeout - tonumber(ARGV[3]) - tonumber(ARGV[4]);" +
            "end;" +

            // add the thread to the queue at the end, and set its timeout in the timeout set to the timeout of
            // the prior thread in the queue (or the timeout of the lock if the queue is empty) plus the
            // threadWaitTime
            "local lastThreadId = redis.call('lindex', KEYS[2], -1);" +
            "local ttl;" +
            "if lastThreadId ~= false and lastThreadId ~= ARGV[2] then " +
            "ttl = tonumber(redis.call('zscore', KEYS[3], lastThreadId)) - tonumber(ARGV[4]);" +
            "else " +
            "ttl = redis.call('pttl', KEYS[1]);" +
            "end;" +
            "local timeout = ttl + tonumber(ARGV[3]) + tonumber(ARGV[4]);" +
            "if redis.call('zadd', KEYS[3], timeout, ARGV[2]) == 1 then " +
            "redis.call('rpush', KEYS[2], ARGV[2]);" +
            "end;" +
            "return ttl;";



    /*public void lock(String key, long time, TimeUnit timeUnit) {
        RedisScript<Long> redisScript = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
        List<String> keys = Arrays.asList(key, prefixName("redisson_lock_queue", key), prefixName("redisson_lock_timeout", key));
        long internalLockLeaseTime = timeUnit.toMillis(time);
        redisTemplate.execute(redisScript, keys, String.valueOf(internalLockLeaseTime), String.valueOf(Thread.currentThread().getId()), String.valueOf(threadWaitTime), String.valueOf(System.currentTimeMillis()));
    }*/

    private ThreadLocal<String> threadLocal = new ThreadLocal<>();

    private static final String PREFIX_LIST_QUEUE_NAME = "distributed_lock_queue:";

    private static final String PREFIX_KEY_NAME = "REDIS:DISTRIBUTE:LOCK:";

    public boolean lock(String key, long time, TimeUnit timeUnit) {
        /**
         * 先判断队列是否有等待的，如果有，排队
         * 没有则尝试获取锁，获取失败进队列
         */
        String listKey = PREFIX_LIST_QUEUE_NAME + key;
        Long listSize = redisTemplate.opsForList().size(listKey);
        String value = key + ":" + Thread.currentThread().getId() + ":" + System.nanoTime();
        LOGGER.info("value = {}, listSize = {}", value, listSize);
        if (listSize == null || listSize == 0) {
            // 先入对再获取锁
            redisTemplate.opsForList().rightPush(listKey, value);
            String nanoTime = String.valueOf(System.nanoTime());
            LOGGER.info(Thread.currentThread().getId() + "==首次加锁==" + nanoTime);
            threadLocal.set(nanoTime);
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, nanoTime, time, timeUnit);
            if (result != null && result) {
                // list为空时是自动删除该key
                redisTemplate.opsForList().leftPop(listKey);
                // 获取锁成功
                return true;
            }
        } else {
            // 已经有了，直接入队
            redisTemplate.opsForList().rightPush(listKey, value);
        }
        while (true) {
            String result = redisTemplate.opsForList().index(listKey, 0);
            LOGGER.info("pushValue = {}, popValue = {}", value, result);
            if (StringUtils.isEquals(value, result)) {
                String nanoTime = String.valueOf(System.nanoTime());
                threadLocal.set(nanoTime);
                LOGGER.info(Thread.currentThread().getId() + "==重试获取锁==" + nanoTime);
                Boolean success = redisTemplate.opsForValue().setIfAbsent(key, nanoTime, time, timeUnit);
                if (success != null && success) {
                    redisTemplate.opsForList().leftPop(listKey);
                    // 获取锁成功
                    return true;
                }
            }
        }
    }

    /**
     * 释放锁
     */
    public void unlock(String key) {
        try {
            String nanoTime = threadLocal.get();
            LOGGER.info(Thread.currentThread().getId() + "==释放锁==" + nanoTime);
            // 如果业务执行时间过长导致锁自动释放(key时间过期自动删除),当前线程认为自己当前还持有锁
            RedisScript<Boolean> redisScript = new DefaultRedisScript<>("if redis.call('get', KEYS[1]) == KEYS[2] then return redis.call('del', KEYS[1]) else return 0 end", Boolean.class);
            redisTemplate.execute(redisScript, Arrays.asList(key, nanoTime));
        } finally {
            threadLocal.remove();
        }
    }


    public boolean lock(String key, long time) {
        return lock(key, time, TimeUnit.SECONDS);
    }


    public static String prefixName(String prefix, String name) {
        if (name.contains("{")) {
            return prefix + ":" + name;
        }
        return prefix + ":{" + name + "}";
    }
}
