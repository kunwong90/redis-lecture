package com.redis.lecture.lock;

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

    private static final String PREFIX_ZSET_NAME = "distributed_lock_timeout:";

    /**
     * 每次启动生成的一个标识
     */
    private final UUID uuid;

    private RedisDistributedFairLock() {
        this.uuid = UUID.randomUUID();
    }

    /*private static final String LUA_DELETE_LIST =
            "local list = redis.call('lrange', KEYS[1], 0, -1);" +
            "for index,value in pairs(list) do local prefix = ARGV[1];" +
                "if (string.sub(value, 0, #prefix) ~= prefix) then " +
                    "redis.call('LREM', KEYS[1], 0, value); " +
                "end;" +
            "end;" +
            "redis.call('rpush', KEYS[1], ARGV[2]);";

    public boolean lock(String key, long time) {
        return lock(key, time, TimeUnit.SECONDS);
    }

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
                // 防止意外情况，比如Redis没做持久化导致数据异常丢失
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
    }*/

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

    /*public boolean lockPro(String key, long time) {
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
            Long listLength = redisTemplate.execute(redisScript, Arrays.asList(listKey, hashKey), identifier, value, String.valueOf(System.currentTimeMillis()), String.valueOf(timeUnit.toMillis(time)));
            LOGGER.info("value = {}, size = {}, expire time = {}", value, listLength, (System.currentTimeMillis() + (timeUnit.toMillis(time)) * listLength));
            while (true) {
                try {
                    //String listFirstValue = redisTemplate.opsForList().index(listKey, 0);
                    String listFirstValue = redisTemplate.execute(new DefaultRedisScript<>(
                            "local result = redis.call('LINDEX', KEYS[1], 0);" +
                                    "if (result ~= false) then return result;else redis.call('del', KEYS[2]);return nil;end;", String.class), Arrays.asList(listKey, hashKey));
                    // 防止意外情况，比如Redis没做持久化导致数据异常丢失
                    if (StringUtils.isBlank(listFirstValue)) {
                        LOGGER.info("{} 队列为空", listKey);
                        //redisTemplate.delete(hashKey);
                        return true;
                    }
                    // 从hash上根据field获取value，判断是否过期
                    String millsStr = redisTemplate.execute(new DefaultRedisScript<>(
                            "return redis.call('hget', KEYS[1], KEYS[2]);", String.class), Arrays.asList(hashKey, listFirstValue, listKey));
                    if (StringUtils.isBlank(millsStr)) {
                        // TODO 要不要从list中删除?
                        *//**
     * 走到这里的几种场景:
     * 1.并发时都是和list中的第一个值比较，当第一个获取成功时删除了，其他线程可能还是和第一个值毕节
     * 2.redis开启了持久化，导致数据不一致
     *//*
                        LOGGER.warn("value = {} 获取值为空.hashKey = {}, listFirstValue = {}", value, hashKey, listFirstValue);
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
                        *//*String lua = "local result = redis.call('set', KEYS[1], ARGV[1], 'ex', ARGV[2], 'nx');" +
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
                        }*//*
                        String lua = "local result = redis.call('set', KEYS[1], ARGV[1], 'ex', ARGV[2], 'nx');" +
                                "if (result ~= false) then " +
                                "redis.call('lpop', KEYS[2]); " +
                                "redis.call('hdel', KEYS[3], KEYS[4]);" +
                                "local exists = redis.call('exists', KEYS[2]);" +
                                "if exists == 0 then " +
                                "redis.call('del', KEYS[3]);" +
                                "end;" +
                                "return 1;" +
                                "else return 0;" +
                                "end;";
                        Boolean result = redisTemplate.execute(new DefaultRedisScript<>(lua, Boolean.class), Arrays.asList(key, listKey, hashKey, listFirstValue), value, String.valueOf(timeUnit.toSeconds(time)));
                        if (result != null && result) {
                            threadLocal.set(value);
                            LOGGER.info("success");
                            return result;
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
    }*/


    /**
     * 使用list加过期时间实现
     *
     * @param key
     * @param time
     * @return
     */
    public boolean lock1(String key, long time) {
        String listKey = PREFIX_LIST_QUEUE_NAME + key;
        try {
            String threadId = String.valueOf(Thread.currentThread().getId());
            String value = key + ":" + threadId + ":" + UUID.randomUUID().toString();
            RedisScript<Long> redisScript = new DefaultRedisScript<>(
                    "redis.call('rpush', KEYS[1], ARGV[1]);" +
                            "local listlength = redis.call('llen', KEYS[1]);" +
                            "redis.call('expire', KEYS[1], tonumber(ARGV[2]) * tonumber(listlength));" +
                            "return listlength;", Long.class);
            long expireTime = TimeUnit.SECONDS.toSeconds(time);
            redisTemplate.execute(redisScript, Collections.singletonList(listKey), value, String.valueOf(expireTime));
            while (true) {
                /**
                 * 0表示list队列为空
                 * 1表示获取锁成功
                 * 2表示获取锁失败
                 */
                String lua = "local listFirstValue = redis.call('LINDEX', KEYS[1], 0);" +
                        "if listFirstValue == false then " +
                        "return 0;" +
                        "elseif (listFirstValue == ARGV[1]) then " +
                        "local result = redis.call('set', KEYS[2], ARGV[1], 'ex', ARGV[2], 'nx');" +
                        "if (result ~= false) then " +
                        "redis.call('lpop', KEYS[1]);" +
                        "return 1;" +
                        "else return 2;" +
                        "end;" +
                        "end;";
                try {
                    Long result = redisTemplate.execute(new DefaultRedisScript<>(lua, Long.class),
                            Arrays.asList(listKey, key), value, String.valueOf(expireTime));
                    if (result != null && (result == 0 || result == 1)) {
                        return true;
                    }
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (Exception e) {
                    LOGGER.error("while loop error.", e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("lock error.", e);
            return true;
        }
    }

    /**
     * 使用redis list + zset实现
     *
     * @param key
     * @param time
     * @return
     */
    public boolean lock2(String key, long time) {
        String listKey = PREFIX_LIST_QUEUE_NAME + key;
        String zSetKey = PREFIX_ZSET_NAME + key;
        String prefix = uuid.toString();
        try {
            String threadId = String.valueOf(Thread.currentThread().getId());
            String value = prefix + ":" + key + ":" + threadId + ":" + UUID.randomUUID().toString();
            redisTemplate.execute(new DefaultRedisScript<>(
                    "local list = redis.call('lrange', KEYS[1], 0, -1);" +
                            "for index,value in pairs(list) do local prefix = ARGV[4];" +
                            "if (string.sub(value, 0, #prefix) ~= prefix) then " +
                            "redis.call('LREM', KEYS[1], 0, value);" +
                            "redis.call('ZREM', KEYS[2], value);" +
                            "end;" +
                            "end;" +
                            "redis.call('rpush', KEYS[1], ARGV[1]);" +
                            "local listlength = redis.call('llen', KEYS[1]);" +
                            "redis.call('zadd', KEYS[2], ARGV[2] + ARGV[3] * listlength, ARGV[1]);" +
                            //"local expire = tonumber(ARGV[2]) * tonumber(listlength);" +
                            //"redis.call('expire', KEYS[1], expire);" +
                            //"redis.call('expire', KEYS[2], expire);" +
                            "return listlength;", Long.class), Arrays.asList(listKey, zSetKey), value, String.valueOf(System.currentTimeMillis()), String.valueOf(TimeUnit.SECONDS.toMillis(time)), prefix);
            while (true) {
                /**
                 * 0表示list为空
                 * 1表示zset没找到对应score或score过期
                 * 2表示获取锁成功
                 * 3表示获取锁失败
                 * 4表示value和list中的第一个值不等，需要继续循环
                 */
                String lua = "local listFirstValue = redis.call('lindex', KEYS[1], 0);" +
                        "if (listFirstValue == false) then " +
                        "redis.call('del', KEYS[2]);" +
                        "return 0;" +
                        "end;" +
                        "local expire = redis.call('ZSCORE', KEYS[2], listFirstValue);" +
                        //"redis.log(redis.LOG_NOTICE, 'expire = '..expire..'=='..ARGV[1]);" +
                        "if (expire == false or expire < ARGV[1]) then " +
                        "redis.call('lpop', KEYS[1]);" +
                        "redis.call('zrem', KEYS[2], listFirstValue);" +
                        "return 1;" +
                        "end;" +
                        "if (listFirstValue == ARGV[2]) then " +
                        "local result = redis.call('set', KEYS[3], ARGV[2], 'ex', ARGV[3], 'nx');" +
                        "if (result ~= false) then " +
                        "redis.call('lpop', KEYS[1]);" +
                        "redis.call('zrem', KEYS[2], listFirstValue);" +
                        "return 2;" +
                        "else return 3;" +
                        "end;" +
                        "else return 4;" +
                        "end;";
                try {
                    Long result = redisTemplate.execute(new DefaultRedisScript<>(lua, Long.class),
                            Arrays.asList(listKey, zSetKey, key), String.valueOf(System.currentTimeMillis()), value, String.valueOf(TimeUnit.SECONDS.toSeconds(time)));
                    if (result != null && (result == 0 || result == 1 || result == 2)) {
                        LOGGER.info("result = {}", result);
                        return true;
                    }
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (Exception e) {
                    LOGGER.error("while loop error.", e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("lock error.", e);
            return true;
        }
    }

    /**
     * 使用redis list + zset实现
     * lock2 升级版
     * @param key
     * @param time
     * @return
     */
    /*public boolean lock3(String key, long time) {
        String listKey = PREFIX_LIST_QUEUE_NAME + key;
        String zSetKey = PREFIX_ZSET_NAME + key;
        String prefix = uuid.toString();
        String value = null;
        try {
            String threadId = String.valueOf(Thread.currentThread().getId());
            value = prefix + ":" + key + ":" + threadId + ":" + UUID.randomUUID().toString();
            Long score = redisTemplate.execute(new DefaultRedisScript<>(
                    "local list = redis.call('lrange', KEYS[1], 0, -1);" +
                            "for index,value in pairs(list) do local prefix = ARGV[4];" +
                            "if (string.sub(value, 0, #prefix) ~= prefix) then " +
                            "redis.call('LREM', KEYS[1], 0, value);" +
                            "redis.call('ZREM', KEYS[2], value);" +
                            "end;" +
                            "end;" +
                            "redis.call('rpush', KEYS[1], ARGV[1]);" +
                            "local listlength = redis.call('llen', KEYS[1]);" +
                            "local score = -1;" +
                            "local result = redis.call('get', KEYS[3]);" +
                            //"redis.log(redis.LOG_NOTICE, 'key = '..KEYS[3]..' listlength = '..listlength);" +
                            "if (result == false) then " +
                            //"redis.log(redis.LOG_NOTICE, 'result is null.key = '.. KEYS[3]);" +
                            "local table = redis.call('ZREVRANGE', KEYS[2], 0, -1, 'WITHSCORES');" +
                            "if (next(table) ~= nil) then score = table[2] + ARGV[3];" +
                            "else " +
                            "score = ARGV[2] + ARGV[3] * listlength;" +
                            //"redis.log(redis.LOG_NOTICE, 'zset is null.key = '..KEYS[3]..' score = '..score);" +
                            "end;" +
                            "else " +
                            //"redis.log(redis.LOG_NOTICE, 'key = '..KEYS[3]..' result = '..result);" +
                            "score = result + ARGV[3] * listlength;" +
                            "end;" +
                            //"redis.log(redis.LOG_NOTICE, 'key = '..KEYS[3]..' score = '..score);" +
                            "redis.call('zadd', KEYS[2], score, ARGV[1]);" +
                            "return tonumber(score);", Long.class), Arrays.asList(listKey, zSetKey, key), value, String.valueOf(System.currentTimeMillis()), String.valueOf(TimeUnit.SECONDS.toMillis(time)), prefix);
            System.out.println("key = " + key + " value = " + value);
            while (true) {
                *//**
     * 0表示list为空
     * 1表示zset没找到对应score或score过期
     * 2表示获取锁成功
     * 3表示获取锁失败
     * 4表示value和list中的第一个值不等，需要继续循环
     *//*
                String lua = "local listFirstValue = redis.call('lindex', KEYS[1], 0);" +
                        "if (listFirstValue == false) then " +
                        "redis.call('del', KEYS[2]);" +
                        "return 0;" +
                        "end;" +
                        "local expire = redis.call('ZSCORE', KEYS[2], listFirstValue);" +
                        "redis.log(redis.LOG_NOTICE, 'expire = '..expire..'=='..ARGV[1]..' value = '..ARGV[2]..' isexpire = '..(tonumber(expire) < tonumber(ARGV[1]) and ' true' or ' false'));" +
                        "if (expire == false or tonumber(expire) < tonumber(ARGV[1])) then " +
                        //"local expirePopVlaue = redis.call('lpop', KEYS[1]);" +
                        "redis.call('lrem', KEYS[1], 0, listFirstValue);" +
                        "redis.call('zrem', KEYS[2], listFirstValue);" +
                        "redis.log(redis.LOG_NOTICE, 'listFirstValue == ARGV[2] '..(listFirstValue == ARGV[2] and ' true' or ' false')..' listFirstValue = '..listFirstValue..' ARGV[2] = '..ARGV[2]);" +
                        "return 1;" +
                        "end;" +
                        "if (listFirstValue == ARGV[2]) then " +
                        "local result = redis.call('set', KEYS[3], ARGV[4], 'ex', ARGV[3], 'nx');" +
                        "if (result ~= false) then " +
                        //"local popvalue = redis.call('lpop', KEYS[1]);" +
                        "redis.call('lrem', KEYS[1], 0, listFirstValue);" +
                        "redis.call('zrem', KEYS[2], listFirstValue);" +
                        "redis.log(redis.LOG_NOTICE, 'listFirstValue = '..listFirstValue..' zrem value = '..listFirstValue..(ARGV[2] == listFirstValue and ' true' or ' false'));" +
                        "return 2;" +
                        "else return 3;" +
                        "end;" +
                        "else return 4;" +
                        "end;";
                try {
                    Long result = redisTemplate.execute(new DefaultRedisScript<>(lua, Long.class),
                            Arrays.asList(listKey, zSetKey, key), String.valueOf(System.currentTimeMillis()), value, String.valueOf(TimeUnit.SECONDS.toSeconds(time)), String.valueOf(score));
                    if (result != null && (result == 0 || result == 1 || result == 2)) {
                        LOGGER.info("result = {}, value = {}", result, value);
                        return true;
                    }
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (Exception e) {
                    LOGGER.error("while loop error.value = {}", value, e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("lock error. value = {}", value, e);
            return true;
        }
    }*/


    /**
     * 使用redis list + zset实现
     * lock2 升级版,解决了lock2并发时过期时间不准确的问题
     * 例如同一个key，第一次获取锁成功删除了list，list length变成0，此时第二个计算过期时间可能就会小于2s
     * <p>
     * 已知问题：
     * 在使用redis持久化时，被删除的key可能会被重新加载到内存中，导致部分key不能被删除
     *
     * @param key
     * @param time
     * @return
     */
    public boolean lock4(String key, long time) {
        String listKey = PREFIX_LIST_QUEUE_NAME + key;
        String zSetKey = PREFIX_ZSET_NAME + key;
        String prefix = uuid.toString();
        try {
            String threadId = String.valueOf(Thread.currentThread().getId());
            String value = prefix + ":" + key + ":" + threadId + ":" + UUID.randomUUID().toString();
            Long score = redisTemplate.execute(new DefaultRedisScript<>(
                    "local list = redis.call('lrange', KEYS[1], 0, -1);" +
                            "for index,value in pairs(list) do local prefix = ARGV[4];" +
                            // 这里解决应用重启遗留的数据
                            "if (string.sub(value, 0, #prefix) ~= prefix) then " +
                            "redis.call('LREM', KEYS[1], 0, value);" +
                            "redis.call('ZREM', KEYS[2], value);" +
                            "end;" +
                            "end;" +
                            "redis.call('rpush', KEYS[1], ARGV[1]);" +
                            "local listlength = redis.call('llen', KEYS[1]);" +
                            "local score = -1;" +
                            "local result = redis.call('get', KEYS[3]);" +
                            //"redis.log(redis.LOG_NOTICE, 'key = '..KEYS[3]..' listlength = '..listlength);" +
                            "if (result == false) then " +
                            //"redis.log(redis.LOG_NOTICE, 'result is null.key = '.. KEYS[3]);" +
                            "local table = redis.call('ZREVRANGE', KEYS[2], 0, -1, 'WITHSCORES');" +
                            "if (next(table) ~= nil) then score = table[2] + ARGV[3];" +
                            "else " +
                            "score = ARGV[2] + ARGV[3] * listlength;" +
                            //"redis.log(redis.LOG_NOTICE, 'zset is null.key = '..KEYS[3]..' score = '..score);" +
                            "end;" +
                            "else " +
                            //"redis.log(redis.LOG_NOTICE, 'key = '..KEYS[3]..' result = '..result);" +
                            "score = result + ARGV[3] * listlength;" +
                            "end;" +
                            //"redis.log(redis.LOG_NOTICE, 'key = '..KEYS[3]..' score = '..score);" +
                            "redis.call('zadd', KEYS[2], score, ARGV[1]);" +
                            "return tonumber(score);", Long.class), Arrays.asList(listKey, zSetKey, key), value, String.valueOf(System.currentTimeMillis()), String.valueOf(TimeUnit.SECONDS.toMillis(time)), prefix);
            //System.out.println("key = " + key + " value = " + value);
            while (true) {
                /**
                 * 0表示list为空
                 * 1表示zset没找到对应score或score过期
                 * 2表示获取锁成功
                 * 3表示获取锁失败
                 * 4表示value和list中的第一个值不等，需要继续循环
                 */
                String lua = "local listFirstValue = redis.call('lindex', KEYS[1], 0);" +
                        "if (listFirstValue == false) then " +
                        "redis.call('del', KEYS[2]);" +
                        "return 0;" +
                        "end;" +
                        "local expire = redis.call('ZSCORE', KEYS[2], listFirstValue);" +
                        //"redis.log(redis.LOG_NOTICE, 'expire = '..expire..'=='..ARGV[1]);" +
                        "if (expire == false or expire < ARGV[1]) then " +
                        "redis.call('lpop', KEYS[1]);" +
                        "redis.call('zrem', KEYS[2], listFirstValue);" +
                        "return 1;" +
                        "end;" +
                        "if (listFirstValue == ARGV[2]) then " +
                        "local result = redis.call('set', KEYS[3], ARGV[4], 'ex', ARGV[3], 'nx');" +
                        "if (result ~= false) then " +
                        "redis.call('lpop', KEYS[1]);" +
                        "redis.call('zrem', KEYS[2], listFirstValue);" +
                        "return 2;" +
                        "else return 3;" +
                        "end;" +
                        "else return 4;" +
                        "end;";
                try {
                    Long result = redisTemplate.execute(new DefaultRedisScript<>(lua, Long.class),
                            Arrays.asList(listKey, zSetKey, key), String.valueOf(System.currentTimeMillis()), value, String.valueOf(TimeUnit.SECONDS.toSeconds(time)), String.valueOf(score));
                    if (result != null && (result == 0 || result == 1 || result == 2)) {
                        LOGGER.info("result = {}", result);
                        return true;
                    }
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (Exception e) {
                    LOGGER.error("while loop error.key = {}", key, e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("lock error. key = {}", key, e);
            return true;
        }
    }
}
