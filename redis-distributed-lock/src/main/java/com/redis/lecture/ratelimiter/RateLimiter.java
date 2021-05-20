package com.redis.lecture.ratelimiter;

public interface RateLimiter {

    boolean rateLimit(String key, int max, int rate);
}
