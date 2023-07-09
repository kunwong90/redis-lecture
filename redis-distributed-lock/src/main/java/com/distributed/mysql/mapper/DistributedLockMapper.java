package com.distributed.mysql.mapper;


import com.distributed.mysql.entity.DistributedLock;

public interface DistributedLockMapper {
    int insert(DistributedLock distributedLock);

    int insertOnDuplicateKeyUpdate(DistributedLock distributedLock);

    int deleteById(Long id);

    int deleteByLockKey(String lockKey);

    DistributedLock existsLockKey(String lockKey);

    int updateByIdWithVersion(DistributedLock distributedLock);
}
