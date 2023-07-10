package com.distributed.mysql;

import com.distributed.lock.DistributedLock;
import com.redis.lecture.lock.BaseTest;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

public class MysqlDistributedOptimisticLockTest extends BaseTest {


    @Resource(name = "mysqlDistributedOptimisticLock")
    private DistributedLock distributedLock;


    String key = "test";

    String value = "value";

    @Test
    public void testLock() {
        boolean result = distributedLock.lock(key, 60, TimeUnit.SECONDS);
        System.out.println("获取锁" + (result ? "成功" : "失败"));
    }


    @Test
    public void testUnlock() {
        boolean result = distributedLock.unlock(key);
        System.out.println("释放锁" + (result ? "成功" : "失败"));
    }

    @Test
    public void testLock1() {
        boolean result = distributedLock.lock(key, value, 60, TimeUnit.SECONDS);
        System.out.println("获取锁" + (result ? "成功" : "失败"));
    }


    @Test
    public void testUnlock1() {
        boolean result = distributedLock.unlock(key, value);
        System.out.println("释放锁" + (result ? "成功" : "失败"));
    }
}