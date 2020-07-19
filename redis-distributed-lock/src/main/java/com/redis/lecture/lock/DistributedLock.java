package com.redis.lecture.lock;

import java.util.concurrent.TimeUnit;

public interface DistributedLock {
    boolean lock(String key, long time, TimeUnit unit);

    boolean unlock(String key);
}
