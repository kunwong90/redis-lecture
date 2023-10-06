package com.distributed.mysql;

import com.distributed.mysql.mapper.DistributedLockMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.concurrent.TimeUnit;


@Service
public class MysqlDistributedLock extends AbstractMysqlDistributedLock {

    private static final Logger LOGGER = LoggerFactory.getLogger(MysqlDistributedLock.class);
    @Resource
    private DistributedLockMapper distributedLockMapper;

    @Override
    public boolean tryLock(String key, String value, long leaseTime, TimeUnit timeUnit) {
        com.distributed.mysql.entity.DistributedLock distributedLock = new com.distributed.mysql.entity.DistributedLock();
        distributedLock.setLockKey(key);
        distributedLock.setLockValue(value);
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
}
