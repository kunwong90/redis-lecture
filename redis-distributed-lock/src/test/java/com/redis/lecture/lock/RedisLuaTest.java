package com.redis.lecture.lock;

import org.junit.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class RedisLuaTest extends BaseTest {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    private ThreadLocal<String> threadLocal = new ThreadLocal<>();

    @Test
    public void test1() {
        /**
         * "local exists = redis.call('exists', KEYS[1]);" +
         *  存在返回1，不存在返回0
         */

        // set 失败返回false
        String lua = "local result = redis.call('set', KEYS[1], ARGV[1], 'ex', ARGV[2], 'nx');" +
                "if result ~= false then " +
                "return 1;" +
                "else " +
                "return 0;" +
                "end;";

        /**
         * 成功返回1，失败返回0
         */
        /*String lua = "local result = redis.call('setnx', KEYS[1], ARGV[1]) " +
                "if result == 1 then " +
                "return 1;" +
                "else " +
                "return 0;" +
                "end;";*/
        RedisScript<Long> redisScript = new DefaultRedisScript<>(lua, Long.class);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList("test1"), UUID.randomUUID().toString(), String.valueOf(12));
        System.out.println(result);
    }

    @Test
    public void test2() {
        String lua =
                "local result = redis.call('setnx', KEYS[1], ARGV[1]);" +
                        "redis.log(redis.LOG_NOTICE, result);" +
                        "if result == 1 then " +
                        "redis.log(redis.LOG_NOTICE, 'set success')" +
                        "redis.call('expire', KEYS[1], 10);" +
                        "end;";
        RedisScript<Void> redisScript = new DefaultRedisScript<>(lua, Void.class);
        redisTemplate.execute(redisScript, Collections.singletonList("test1"), UUID.randomUUID().toString());
    }

    @Test
    public void test3() {
        /**
         * 指定key不存在返回0
         */
        String lua =
                "local result = 1;" +
                        "while true do " +
                        "local firstThreadId2 = redis.call('lindex', KEYS[1], 0);" +
                        "if firstThreadId2 == false then " +
                        "result = 0; " +
                        "break;" +
                        "else " +
                        "result = 2; " +
                        "break;" +
                        "end;" +
                        "end;" +
                        "return result;";
        RedisScript<Long> redisScript = new DefaultRedisScript<>(lua, Long.class);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList("test1"));
        System.out.println(result);
    }

    @Test
    public void test4() {
        /**
         * KEYS[1] list的key
         * KEYS[2] 是传入的key
         * ARGV[1] 是应用启动生成的标识
         * ARGV[2] 是 KEYS[2]对应的value
         * ARGV[3] 是KEYS[2]的过期时间
         */
        String lua =
                "local list = redis.call('lrange', KEYS[1], 0, -1);" +
                        "for index,value in pairs(list) do local prefix = ARGV[1]; " +
                        "if (string.sub(value, 0, #prefix) ~= prefix) then " +
                        "redis.call('LREM', KEYS[1], 0, value);" +
                        "end;" +
                        "end;" +
                        "redis.log(redis.LOG_NOTICE, 'rpush value = '..ARGV[2]);" +
                        "redis.call('rpush', KEYS[1], ARGV[2]);" +
                        "while true do " +
                        "local firstValue = redis.call('lindex', KEYS[1], 0);" +
                        "redis.log(redis.LOG_NOTICE, 'firstValue = '..firstValue);" +
                        "if (firstValue == false) then break;" +
                        "elseif (firstValue == ARGV[2]) then " +
                        "local r = redis.call('get', KEYS[2]);" +
                        "redis.log(redis.LOG_NOTICE, 'get key = '..(r ~= false and r or 'empty'));" +
                        "if (redis.call('set', KEYS[2], ARGV[2], 'ex', ARGV[3], 'nx') ~= false) then " +
                        "redis.log(redis.LOG_NOTICE, 'value = '..ARGV[2]..' set success');" +
                        //"redis.call('expire', KEYS[2], ARGV[3]);" +
                        "redis.call('lpop', KEYS[1]);" +
                        "break;" +
                        "end;" +
                        "end;" +
                        "end;";

        IntStream.range(0, 3).parallel().forEach(value -> {
            RedisScript<Void> redisScript = new DefaultRedisScript<>(lua, Void.class);
            String key = "test";
            String argv1 = UUID.randomUUID().toString();
            String argv2 = argv1 + ":" + key + ":" + Thread.currentThread().getId() + ":" + UUID.randomUUID().toString();
            String argv3 = String.valueOf(20);
            System.out.println("==================================");
            redisTemplate.execute(redisScript, Arrays.asList("distributed_lock_queue:" + key, key), argv1, argv2, argv3);
            System.out.println("success");
            threadLocal.set(argv2);

            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (Exception e) {

            } finally {
                unlock(key);
            }
        });
    }

    @Test
    public void test5() {
        String lua =
                "local list = redis.call('lrange', KEYS[1], 0, -1);" +
                        "for index,value in pairs(list) do local prefix = ARGV[1];" +
                        "if (string.sub(value, 0, #prefix) ~= prefix) then " +
                        //"redis.log(redis.LOG_NOTICE, 'need delete');" +
                        "redis.call('LREM', KEYS[1], 0, value); " +
                        "end;" +
                        "end;" +
                        "redis.call('rpush', KEYS[1], ARGV[2]);";

        RedisScript<Void> redisScript = new DefaultRedisScript<>(lua, Void.class);
        String key = "test";
        String argv1 = UUID.randomUUID().toString();
        String argv2 = argv1 + ":" + key + ":" + Thread.currentThread().getId() + ":" + UUID.randomUUID().toString();
        String argv3 = String.valueOf(30);
        IntStream.range(0, 10).forEach(value -> {
            redisTemplate.execute(redisScript, Arrays.asList("distributed_lock_queue:" + key, key), argv1, argv2, argv3);
        });

    }

    @Test
    public void test6() {
        /**
         * 存在key且有结果返回1，没有key返回0
         */
        String lua = "local result = redis.call('get', KEYS[1]);" +
                "if result ~= false then return 1 else return 0; end;";
        RedisScript<Long> redisScript = new DefaultRedisScript<>(lua, Long.class);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList("test1"));
        System.out.println(result);
    }

    @Test
    public void test7() {
        String lua = "local result = redis.call('get', KEYS[1]);" +
                "redis.log(redis.LOG_NOTICE, 'value = '..(result ~= false and result or 'empty'));" +
                "return result;";
        RedisScript<String> redisScript = new DefaultRedisScript<>(lua, String.class);
        String result = redisTemplate.execute(redisScript, Collections.singletonList("test1"));
        System.out.println(result);
    }

    private void unlock(String key) {
        try {
            String value = threadLocal.get();
            // 如果业务执行时间过长导致锁自动释放(key时间过期自动删除),当前线程认为自己当前还持有锁
            RedisScript<Boolean> redisScript = new DefaultRedisScript<>("if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end;", Boolean.class);
            redisTemplate.execute(redisScript, Collections.singletonList(key), value);
        } catch (Exception e) {
        } finally {
            threadLocal.remove();
        }
    }

    @Test
    public void luaSetNxEx() {
        /**
         * 成功返回1，失败返回0
         */
        String lua = "local result = redis.call('set', KEYS[1], ARGV[1], 'ex', ARGV[2], 'nx');" +
                "if (result ~= false) then return 1; else return 0; end;";
        RedisScript<Long> redisScript = new DefaultRedisScript<>(lua, Long.class);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList("test1"), "value1", String.valueOf(100));
        System.out.println(result);

        RedisScript<Boolean> del = new DefaultRedisScript<>("if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Boolean.class);
        redisTemplate.execute(del, Collections.singletonList("test1"), "value1");
    }


    @Test
    public void llen() {
        /**
         * 成功返回1，失败返回0
         */
        String lua = "return redis.call('llen', KEYS[1]);";
        RedisScript<Long> redisScript = new DefaultRedisScript<>(lua, Long.class);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList("distributed_lock_queue:test01"));
        System.out.println(result);
    }


    @Test
    public void hget() {
        /**
         * 没有返回null
         */
        String lua = "return redis.call('hget', KEYS[1], KEYS[2]);";
        RedisScript<String> redisScript = new DefaultRedisScript<>(lua, String.class);
        String result = redisTemplate.execute(redisScript, Arrays.asList("test1", "field"));
        System.out.println(result);
    }

    @Test
    public void hget1() {
        /**
         * 没有返回null
         */
        String lua = "local result = redis.call('hget', KEYS[1], KEYS[2]);" +
                "if result ~= false then return result; else return nil;end;";
        RedisScript<String> redisScript = new DefaultRedisScript<>(lua, String.class);
        String result = redisTemplate.execute(redisScript, Arrays.asList("test1", "field"));
        System.out.println(result);
    }

    @Test
    public void expireTest() {
        /**
         * lua脚本只能保障执行过程中不被其他线程抢占执行，
         * 不能保证原子操作，如果第二个语句写成了expir
         * 就会导致set命令执行成功而设置过期时间失败，造成key永远不会过期
         */
        String lua = "redis.call('set', KEYS[1], ARGV[1]);" +
                "redis.call('expire', KEYS[1], ARGV[2])";
        RedisScript<Void> redisScript = new DefaultRedisScript<>(lua, Void.class);
        redisTemplate.execute(redisScript, Collections.singletonList("test1"), "value1", String.valueOf(20));
    }

    @Test
    public void listHashTest() {
        String listKey = "listKey1";
        String hashKey = "hashKey";
        String listFirstValue = redisTemplate.execute(new DefaultRedisScript<>(
                "local result = redis.call('LINDEX', KEYS[1], 0);" +
                        "if (result ~= false) then return result;else redis.call('del', KEYS[2]);return nil;end;", String.class), Arrays.asList(listKey, hashKey));
        System.out.println(listFirstValue);
    }

    @Test
    public void luaTest() {
        String key = "key";
        String listKey = "listKey";
        String hashKey = "hashKey";
        String listFirstValue = "listFirstValue";
        String value = "value";
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
        Boolean result = redisTemplate.execute(new DefaultRedisScript<>(lua, Boolean.class), Arrays.asList(key, listKey, hashKey, listFirstValue), value, String.valueOf(200));
        System.out.println(result);
    }

    @Test
    public void lua1Test() {

        String listKey = "listKey";
        String key = "key";
        String value = "value";
        long time = 20;
        String lua = "local listFirstValue = redis.call('LINDEX', KEYS[1], 0);" +
                "if listFirstValue == false then " +
                "return nil;" +
                "elseif (listFirstValue == ARGV[1]) then " +
                "local result = redis.call('set', KEYS[2], ARGV[1], 'ex', ARGV[2], 'nx');" +
                "if (result ~= false) then " +
                "redis.call('lpop', KEYS[1]);" +
                "return 1;" +
                "else return nil;" +
                "end;" +
                "end;";
        Boolean result = redisTemplate.execute(new DefaultRedisScript<>(lua, Boolean.class), Arrays.asList(listKey, key), value, String.valueOf(TimeUnit.SECONDS.toSeconds(time)));
        System.out.println(result);

    }

    @Test
    public void luaTimeTest() {
        /**
         * time
         * 一个包含两个字符串的列表： 第一个字符串是当前时间(以 UNIX 时间戳格式表示)，而第二个字符串是当前这一秒钟已经逝去的微秒数。
         */
        Long time = redisTemplate.execute(new DefaultRedisScript<>("local table = redis.call('time');" +
                "local time = tonumber(table[1])*1000 + tonumber(table[2])/1000;" +
                "return time;", Long.class), Collections.EMPTY_LIST);
        System.out.println(time);
        System.out.println(System.currentTimeMillis());
    }

    @Test
    public void zrangeTest() {
        /**
         * 可以返回score最大值
         */
        String result = redisTemplate.execute(new DefaultRedisScript<>("return redis.call('ZRANGE', 'distributed_lock_timeout:test12', 0, -1, 'WITHSCORES');", String.class), Collections.emptyList());
        System.out.println(result);
    }

    @Test
    public void zrangeTest1() {
        Long result = redisTemplate.execute(new DefaultRedisScript<>(
                "local score = redis.call('ZREVRANGE', 'distributed_lock_timeout:test12', 0, -1, 'WITHSCORES');" +
                        "redis.log(redis.LOG_NOTICE, next(score) == nil and 'true' or 'false');" +
                        "local score1 = -1;" +
                        "if (next(score) ~= nil) then score1 = score[2];end;" +
                        "redis.log(redis.LOG_NOTICE, score1);" +
                        "return tonumber(score1);", Long.class), Collections.emptyList());
        System.out.println(result);
    }

    @Test
    public void luaGet() {
        Long result = redisTemplate.execute(new DefaultRedisScript<>("local result = redis.call('get', KEYS[1]);" +
                "if (result == false) then return tonumber(-1);" +
                "else return tonumber(result);end;", Long.class), Arrays.asList("test"));
        System.out.println(result);
    }

    @Test
    public void hmgetTest() {
        redisTemplate.execute(new DefaultRedisScript<>("local ratelimit_info = redis.pcall('HMGET',KEYS[1],'last_time','current_token');" +
                "redis.log(redis.LOG_NOTICE, next(ratelimit_info) == nil and 'true' or 'false');" +
                "redis.log(redis.LOG_NOTICE, ratelimit_info[1] == false and 'true' or 'false');", Void.class), Arrays.asList("test"));
    }
}
