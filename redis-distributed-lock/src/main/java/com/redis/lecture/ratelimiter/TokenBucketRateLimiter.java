package com.redis.lecture.ratelimiter;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 令牌桶限流算法
 */
@Component
public class TokenBucketRateLimiter implements RateLimiter {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean rateLimit(String key, int max, int rate) {
        List<String> keyList = new ArrayList<>(1);
        keyList.add(key);
        return "1".equals(redisTemplate
                .execute(new RedisReteLimitScript(), keyList, Integer.toString(max), Integer.toString(rate)));
    }
}
