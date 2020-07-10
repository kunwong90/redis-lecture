package com.redis.lecture.lock;

import com.alibaba.fastjson.JSON;
import com.redis.lecture.util.CollectionUtils;
import com.redis.lecture.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * redis实现的分布式公平锁
 */
@Component
public class RedisDistributedFairLock implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDistributedFairLock.class);

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private RedisDistributedLock redisDistributedLock;

    private ThreadLocal<String> threadLocal = new ThreadLocal<>();

    private static final String PREFIX_LIST_QUEUE_NAME = "distributed_lock_queue:";

    //private static final String PREFIX_ZSET_TIMEOUT_NAME = "distributed_lock_timeout:";

    public boolean lock(String key, long time, TimeUnit timeUnit) {
        String listKey = PREFIX_LIST_QUEUE_NAME + key;
        String threadId = String.valueOf(Thread.currentThread().getId());
        String value = key + ":" + threadId + ":" + UUID.randomUUID().toString();
        LOGGER.info("value = " + value);
        redisTemplate.opsForList().rightPush(listKey, value);
        while (true) {
            String result = redisTemplate.opsForList().index(listKey, 0);
            LOGGER.info("pushValue = {}, popValue = {}", value, result);
            if (StringUtils.isEquals(value, result)) {
                Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, time, timeUnit);
                if (success != null && success) {
                    threadLocal.set(value);
                    LOGGER.info("==自旋获取锁成功== " + value);
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


    public static String prefixName(String prefix, String name) {
        if (name.contains("{")) {
            return prefix + ":" + name;
        }
        return prefix + ":{" + name + "}";
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            Set<String> queueKeys = scan(PREFIX_LIST_QUEUE_NAME);
            //LOGGER.info("需要删除的keys = {}", JSON.toJSONString(queueKeys));
            System.out.println("需要删除的keys = " + JSON.toJSONString(queueKeys));
            if (CollectionUtils.isNotEmpty(queueKeys)) {
                redisTemplate.delete(queueKeys);
            }
        } catch (Exception e) {
            LOGGER.error("scan exception", e);
        }
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
