package com.redis.lecture.pipeline;

import com.redis.lecture.lock.BaseTest;
import org.junit.Test;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedisPipelineServiceTest extends BaseTest {

    @Resource
    private RedisPipelineService redisPipelineService;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Test
    public void testPipelineGet() {
        String keyPrefix = "PIPELINE:TEST:KEY:";
        String valuePrefix = "PIPELINE:TEST:VALUE:";
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            keys.add(keyPrefix + i);
        }
        long start = System.currentTimeMillis();
        List<Object> list = redisPipelineService.pipelineGet(keys);


        String key = "PIPELINE:TEST:KEY:22";
        int index = keys.indexOf(key);
        if (index != -1) {
            System.out.println(list.get(index));
        } else {
            System.out.println("key " + key + " 不存在");
        }
        System.out.println("PipelineGet cost = " + (System.currentTimeMillis() - start));
    }


    @Test
    public void testPipelinePut() {
        String keyPrefix = "PIPELINE:TEST:KEY:";
        String valuePrefix = "PIPELINE:TEST:VALUE:";
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < 10000; i++) {
            map.put(keyPrefix + i, valuePrefix + i);
        }
        long start = System.currentTimeMillis();
        redisPipelineService.pipelinePut(map);
        System.out.println("PipelinePut cost = " + (System.currentTimeMillis() - start));
    }


    @Test
    public void testMultiPut() {
        String keyPrefix = "PIPELINE:TEST:KEY:";
        String valuePrefix = "PIPELINE:TEST:VALUE:";
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            redisTemplate.opsForValue().set(keyPrefix + i, valuePrefix + i);
        }
        System.out.println("MultiPut cost = " + (System.currentTimeMillis() - start));
    }


    @Test
    public void testMultiGet() {
        String keyPrefix = "PIPELINE:TEST:KEY:";
        String valuePrefix = "PIPELINE:TEST:VALUE:";
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            redisTemplate.opsForValue().get(keyPrefix + i);
        }
        System.out.println("MultiGet cost = " + (System.currentTimeMillis() - start));
    }
}