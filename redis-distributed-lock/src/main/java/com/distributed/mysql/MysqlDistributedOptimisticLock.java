package com.distributed.mysql;

import com.distributed.lock.DistributedLock;
import com.distributed.mysql.mapper.DistributedLockMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MySQL 基于乐观锁实现的分布式锁
 */
@Service
public class MysqlDistributedOptimisticLock implements DistributedLock {

    private static final Logger LOGGER = LoggerFactory.getLogger(MysqlDistributedOptimisticLock.class);
    @Resource
    private DistributedLockMapper distributedLockMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean lock(String key, long leaseTime, TimeUnit timeUnit) {
        com.distributed.mysql.entity.DistributedLock distributedLock;
        try {

            distributedLock = distributedLockMapper.existsLockKey(key);
            if (distributedLock == null) {
                // 不存在则插入
                distributedLock = new com.distributed.mysql.entity.DistributedLock();
                distributedLock.setLockKey(key);
                distributedLock.setLockValue(UUID.randomUUID().toString());
                leaseTime = timeUnit.toSeconds(leaseTime);
                distributedLock.setLeaseTime(leaseTime);
                distributedLock.setExpireDate(new Date(System.currentTimeMillis() + leaseTime * 1000));
                distributedLock.setVersion(1);
                distributedLock.setReentrantTimes(1);
                int count = distributedLockMapper.insert(distributedLock);
                LOGGER.info("key = {} 插入成功", key);
                return count > 0;
            } else {
                // 存在首先判断是否已过期,没有过期获取锁失败,过期则更新数据
                Date expireDate = distributedLock.getExpireDate();
                if (expireDate.before(new Date())) {
                    // 没有过期,获取锁失败
                    return false;
                } else {
                    // 过期,更新数据
                    distributedLock.setLockValue(UUID.randomUUID().toString());
                    leaseTime = timeUnit.toSeconds(leaseTime);
                    distributedLock.setLeaseTime(leaseTime);
                    distributedLock.setExpireDate(new Date(System.currentTimeMillis() + leaseTime * 1000));
                    int count = distributedLockMapper.updateByIdWithVersion(distributedLock);
                    LOGGER.info("key = {} 受影响行数 = {}", key, count);
                    if (count == 0) {
                        LOGGER.info("key = {} 更新失败", key);
                    } else if (count == 1) {
                        LOGGER.info("key = {} 更新成功", key);
                    }
                    return count > 0;
                }
            }
        } catch (Exception e) {
            LOGGER.error("key = " + key + " 执行数据库异常", e);
            return true;
        }
    }

    @Override
    public boolean unlock(String key) {
        try {
            return distributedLockMapper.deleteByLockKey(key) > 0;
        } catch (Exception e) {
            LOGGER.error("key = " + key + " 执行数据库异常", e);
            return true;
        }
    }
}
