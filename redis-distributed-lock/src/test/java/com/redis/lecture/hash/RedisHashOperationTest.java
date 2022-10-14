package com.redis.lecture.hash;

import com.redis.lecture.lock.BaseTest;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RedisHashOperationTest extends BaseTest {

    @Resource
    private RedisHashOperation redisHashOperation;

    @Test
    public void hsetWithExpire() {
        String key = "hashtest";
        String filed = "field144";
        String value = "value1333";
        boolean result = redisHashOperation.hsetWithExpire(key, filed, value, TimeUnit.SECONDS, 100);
        System.out.println(result);
    }

    @Test
    public void hmsetWithExpire() {
        String key = "hmsetkey";
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < 100000; i++) {
            map.put("key" + i, "value" + i);
        }
        long start = System.currentTimeMillis();
        boolean result = redisHashOperation.hmsetWithExpire(key, map, TimeUnit.SECONDS, 0);
        System.out.println(result);
        System.out.println(System.currentTimeMillis() - start);
    }
}