package com.redis.lecture.info;

import com.redis.lecture.lock.BaseTest;
import org.junit.Test;

import javax.annotation.Resource;

public class ServerInfoTest extends BaseTest {

    @Resource
    private ServerInfo serverInfo;

    @Test
    public void testGetClientList() {
        serverInfo.getClientList();
    }

    @Test
    public void testGetRedisServerMemoryInfo() {
        serverInfo.getRedisServerMemoryInfo();
    }

    @Test
    public void testGetRedisServerInfo() {
        serverInfo.getRedisServerInfo();
    }

    @Test
    public void testGetRedisPersistence() {
        serverInfo.getRedisPersistence();
    }

    @Test
    public void testGetRedisStats() {
        serverInfo.getRedisStats();
    }

    @Test
    public void testGetReplication() {
        serverInfo.getReplication();
    }

    @Test
    public void testGetRedisCPUInfo() {
        serverInfo.getRedisCPUInfo();
    }

    @Test
    public void testGetCluster() {
        serverInfo.getCluster();
    }

    @Test
    public void testGetKeyspace() {
        serverInfo.getKeyspace();
    }
}