package com.redis.lecture.pipeline;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class RedisPipelineService {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    public List<Object> pipelineGet(List<String> keys) {
        return redisTemplate.executePipelined((RedisCallback<String>) redisConnection -> {
            keys.forEach(key -> {
                redisConnection.get(key.getBytes(StandardCharsets.UTF_8));
            });
            return null;
        });
    }


    public void pipelinePut(Map<String, String> pairs) {
        redisTemplate.executePipelined((RedisCallback<String>) redisConnection -> {
            pairs.entrySet().forEach(entry -> redisConnection.set(entry.getKey().getBytes(StandardCharsets.UTF_8), entry.getValue().getBytes(StandardCharsets.UTF_8)));
            return null;
        });
    }
}
