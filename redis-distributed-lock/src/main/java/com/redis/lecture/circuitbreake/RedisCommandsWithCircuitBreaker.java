package com.redis.lecture.circuitbreake;

import org.springframework.stereotype.Component;

@Component
public class RedisCommandsWithCircuitBreaker extends DefaultRedisCommands {

    private final CircuitBreaker circuitBreaker = new CircuitBreaker(new Config());


    @Override
    public boolean set(String key, String value) {
        return circuitBreaker.run(() -> {
            super.set(key, value);
            return true;
        }, throwable -> false);


    }

    @Override
    public String get(String key) {
        return circuitBreaker.run(() -> super.get(key), throwable -> null);
    }


}
