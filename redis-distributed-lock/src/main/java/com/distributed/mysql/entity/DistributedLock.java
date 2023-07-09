package com.distributed.mysql.entity;

import java.util.Date;

public class DistributedLock {

    private Long id;
    /**
     * 唯一标识
     */
    private String lockKey;
    /**
     * 唯一标识对应值
     */
    private String lockValue;
    /**
     * key持有锁时间
     */
    private long leaseTime;
    /**
     * key过期时间
     */
    private Date expireDate;

    private Integer reentrantTimes;

    private Integer version;
    /**
     * 添加时间
     */
    private Date addTime;
    /**
     * 更新时间
     */
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLockKey() {
        return lockKey;
    }

    public void setLockKey(String lockKey) {
        this.lockKey = lockKey;
    }

    public String getLockValue() {
        return lockValue;
    }

    public void setLockValue(String lockValue) {
        this.lockValue = lockValue;
    }

    public long getLeaseTime() {
        return leaseTime;
    }

    public void setLeaseTime(long leaseTime) {
        this.leaseTime = leaseTime;
    }

    public Date getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(Date expireDate) {
        this.expireDate = expireDate;
    }

    public Integer getReentrantTimes() {
        return reentrantTimes;
    }

    public void setReentrantTimes(Integer reentrantTimes) {
        this.reentrantTimes = reentrantTimes;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Date getAddTime() {
        return addTime;
    }

    public void setAddTime(Date addTime) {
        this.addTime = addTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
