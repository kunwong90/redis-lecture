package com.distributed.mysql;

import com.distributed.lock.DistributedLock;
import com.distributed.mysql.mapper.DistributedLockMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public abstract class AbstractMysqlDistributedLock implements DistributedLock {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMysqlDistributedLock.class);
    @Resource
    private DistributedLockMapper distributedLockMapper;


    @Override
    public boolean lock(String key, long leaseTime, TimeUnit timeUnit) {
        com.distributed.mysql.entity.DistributedLock distributedLock = new com.distributed.mysql.entity.DistributedLock();
        distributedLock.setLockKey(key);
        distributedLock.setLockValue(UUID.randomUUID().toString());
        leaseTime = timeUnit.toSeconds(leaseTime);
        distributedLock.setLeaseTime(leaseTime);
        distributedLock.setExpireDate(new Date(System.currentTimeMillis() + leaseTime * 1000));
        try {
            int count = distributedLockMapper.insertOnDuplicateKeyUpdate(distributedLock);
            LOGGER.info("key = {} 受影响行数 = {}", key, count);
            if (count == 0) {
                LOGGER.info("key = {} 唯一索引冲突,数据没有变化", key);
            } else if (count == 1) {
                LOGGER.info("key = {} 插入成功", key);
            } else if (count == 2) {
                LOGGER.info("key = {} 唯一索引冲突,更新数据成功", key);
            }
            return count > 0;
        } catch (Exception e) {
            LOGGER.error("key = " + key + " 执行数据库异常", e);
            return true;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean lock(String key, String value, long leaseTime, TimeUnit timeUnit) {
        return tryLock(key, value, leaseTime, timeUnit);
    }

    public abstract boolean tryLock(String key, String value, long leaseTime, TimeUnit timeUnit);

    @Override
    public boolean unlock(String key) {
        try {
            int count = distributedLockMapper.deleteByLockKey(key);
            LOGGER.info("key = {},删除影响行数 = {}", key, count);
            return count > 0;
        } catch (Exception e) {
            LOGGER.error("解锁异常,key = " + key, e);
            return true;
        }
    }

    @Override
    public boolean unlock(String key, String value) {
        try {
            int count = distributedLockMapper.deleteByLockKeyAndLockValue(key, value);
            LOGGER.info("key = {},value = {} 删除影响行数 = {}", key, value, count);
            return count > 0;
        } catch (Exception e) {
            LOGGER.error("解锁异常,key = " + key + " ,value = " + value, e);
            return true;
        }
    }
}
