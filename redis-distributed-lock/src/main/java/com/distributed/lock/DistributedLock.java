package com.distributed.lock;

import java.util.concurrent.TimeUnit;

public interface DistributedLock {
    boolean lock(String key, long leaseTime, TimeUnit timeUnit);

    boolean unlock(String key);

    boolean lock(String key, String value, long leaseTime, TimeUnit timeUnit);

    boolean unlock(String key, String value);
}
