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
}