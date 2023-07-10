package com.distributed.mysql.mapper;


import com.distributed.mysql.entity.DistributedLock;
import org.apache.ibatis.annotations.Param;

public interface DistributedLockMapper {
    int insert(DistributedLock distributedLock);

    int insertOnDuplicateKeyUpdate(DistributedLock distributedLock);

    int deleteById(Long id);

    int deleteByLockKey(String lockKey);

    int deleteByLockKeyAndLockValue(@Param("lockKey") String lockKey, @Param("lockValue") String lockValue);

    DistributedLock existsLockKey(String lockKey);

    int updateByIdWithVersion(DistributedLock distributedLock);
}
