package com.distributed.mysql;

import com.distributed.mysql.mapper.DistributedLockMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * MySQL 基于乐观锁实现的分布式锁
 */
@Service
public class MysqlDistributedOptimisticLock extends AbstractMysqlDistributedLock {

    private static final Logger LOGGER = LoggerFactory.getLogger(MysqlDistributedOptimisticLock.class);
    @Resource
    private DistributedLockMapper distributedLockMapper;

    @Override
    public boolean tryLock(String key, String value, long leaseTime, TimeUnit timeUnit) {
        com.distributed.mysql.entity.DistributedLock distributedLock;
        try {

            distributedLock = distributedLockMapper.existsLockKey(key);
            if (distributedLock == null) {
                // 不存在则插入
                distributedLock = new com.distributed.mysql.entity.DistributedLock();
                distributedLock.setLockKey(key);
                distributedLock.setLockValue(value);
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
                    distributedLock.setLockValue(value);
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
        } catch (DuplicateKeyException e) {
            LOGGER.error("插入或更新导致主键或唯一索引冲突", e);
            return false;
        } catch (Exception e) {
            LOGGER.error("key = " + key + " 执行数据库异常", e);
            return true;
        }
    }
}
