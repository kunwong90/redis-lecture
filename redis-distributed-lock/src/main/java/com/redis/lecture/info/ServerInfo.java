package com.redis.lecture.info;

import com.redis.lecture.util.CollectionUtils;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisSentinelConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.RedisClientInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Properties;

@Service
public class ServerInfo {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    public void getClientList() {
        List<RedisClientInfo> redisClientInfos = redisTemplate.getClientList();
        if (CollectionUtils.isNotEmpty(redisClientInfos)) {
            redisClientInfos.forEach(System.out::println);
        }

        Properties properties = redisTemplate.getRequiredConnectionFactory().getConnection().info();
        System.out.println(properties);

        RedisClusterConnection redisClusterConnection = redisTemplate.getRequiredConnectionFactory().getClusterConnection();
        System.out.println(redisClusterConnection);
        RedisSentinelConnection redisSentinelConnection = redisTemplate.getRequiredConnectionFactory().getSentinelConnection();
        System.out.println(redisSentinelConnection);


    }
}
