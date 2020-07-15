package com.redis.lecture.lock;

import org.apache.commons.lang3.StringUtils;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
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

    private static final String PREFIX_HASH_NAME = "distributed_lock_timeout:";

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
            while (true) {
                String listFirstValue = redisTemplate.opsForList().index(listKey, 0);
                // 防止以为情况，比如Redis没做持久化导致数据异常丢失
                if (StringUtils.isBlank(listFirstValue)) {
                    return true;
                }
                if (StringUtils.equals(value, listFirstValue)) {
                    String lua = "local result = redis.call('set', KEYS[1], ARGV[1], 'ex', ARGV[2], 'nx');" +
                            "if (result ~= false) then return 1; else return 0; end;";
                    Long result = redisTemplate.execute(new DefaultRedisScript<>(lua, Long.class), Collections.singletonList(key), value, String.valueOf(timeUnit.toSeconds(time)));
                    if (result != null && result == 1) {
                        threadLocal.set(value);
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
            return true;
        }
    }

    /**
     * 释放锁
     */
    public void unlock(String key) {
        try {
            String value = threadLocal.get();
            // 如果业务执行时间过长导致锁自动释放(key时间过期自动删除),当前线程认为自己当前还持有锁
            RedisScript<Boolean> redisScript = new DefaultRedisScript<>("if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end;", Boolean.class);
            redisTemplate.execute(redisScript, Collections.singletonList(key), value);
        } catch (Exception e) {
          LOGGER.error("unlock error.", e);
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

    /**
     * lock升级版，做了一些优化
     * @param key
     * @param time
     * @return
     */
    public boolean lockPro(String key, long time) {
        return lockPro(key, time, TimeUnit.SECONDS);
    }
    public boolean lockPro(String key, long time, TimeUnit timeUnit) {
        String listKey = PREFIX_LIST_QUEUE_NAME + key;
        String hashKey = PREFIX_HASH_NAME + key;
        String identifier = uuid.toString();
        try {
            String threadId = String.valueOf(Thread.currentThread().getId());
            String value = identifier + ":" + key + ":" + threadId + ":" + System.currentTimeMillis();
            RedisScript<Long> redisScript = new DefaultRedisScript<>(
                    "local list = redis.call('lrange', KEYS[1], 0, -1);" +
                            "for index,value in pairs(list) do local prefix = ARGV[1];" +
                            "if (string.sub(value, 0, #prefix) ~= prefix) then " +
                            "redis.call('LREM', KEYS[1], 0, value); " +
                            "end;" +
                            "end;" +
                            "redis.call('rpush', KEYS[1], ARGV[2]);" +
                            "local listlength = redis.call('llen', KEYS[1]);" +
                            "redis.call('hset', KEYS[2], ARGV[2], tonumber(ARGV[3]) + tonumber(ARGV[4]) * tonumber(listlength));" +
                            "return listlength;", Long.class);
            Long listLength = redisTemplate.execute(redisScript, Arrays.asList(listKey, hashKey), identifier, value, String.valueOf(System.currentTimeMillis()), String.valueOf(timeUnit.toMillis(time) + 1000));
            LOGGER.info("value = {}, size = {}, expire time = {}", value, listLength, (System.currentTimeMillis() + (timeUnit.toMillis(time) +1000)*listLength));
            while (true) {
                try {
                    String listFirstValue = redisTemplate.opsForList().index(listKey, 0);
                    // 防止以为情况，比如Redis没做持久化导致数据异常丢失
                    if (StringUtils.isBlank(listFirstValue)) {
                        LOGGER.info("{} 队列为空", listKey);
                        redisTemplate.delete(hashKey);
                        return true;
                    }
                    // 从hash上根据field获取value，判断是否过期
                    String millsStr = redisTemplate.execute(new DefaultRedisScript<>("return redis.call('hget', KEYS[1], KEYS[2])", String.class), Arrays.asList(hashKey, listFirstValue));
                    if (StringUtils.isBlank(millsStr)) {
                        // TODO 要不要从list中删除?
                        //LOGGER.info("{}, {} 获取值为空.value = {}", hashKey, listFirstValue, value);
                        continue;
                    }
                    long mills = Long.parseLong(millsStr);
                    if (mills <= System.currentTimeMillis()) {
                        // 过期
                        LOGGER.info("expire.value = {}, expire = {}", listFirstValue, millsStr);
                        redisTemplate.execute(new DefaultRedisScript<>("redis.call('lpop', KEYS[1]);" +
                                "redis.call('hdel', KEYS[2], KEYS[3]);", Void.class), Arrays.asList(listKey, hashKey, listFirstValue));
                        return false;
                    }
                    if (StringUtils.equals(value, listFirstValue)) {
                        String lua = "local result = redis.call('set', KEYS[1], ARGV[1], 'ex', ARGV[2], 'nx');" +
                                "if (result ~= false) then return 1; else return 0; end;";
                        Long result = redisTemplate.execute(new DefaultRedisScript<>(lua, Long.class), Collections.singletonList(key), value, String.valueOf(timeUnit.toSeconds(time)));
                        if (result != null && result == 1) {
                            threadLocal.set(value);
                            redisTemplate.execute(new DefaultRedisScript<>("redis.call('lpop', KEYS[1]); redis.call('hdel', KEYS[2], KEYS[3]);" +
                                    "local exists = redis.call('exists', KEYS[1]);" +
                                    "if exists == 0 then redis.call('del', KEYS[2]); end;", Void.class), Arrays.asList(listKey, hashKey, listFirstValue));
                            //LOGGER.info("value = {} 已被删除", value);
                            // 获取锁成功
                            return true;
                        }
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(200);
                    } catch (Exception ignore) {

                    }
                } catch (Exception e) {
                    LOGGER.error("while loop error.", e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("lock error.", e);
            return true;
        }
    }
}
