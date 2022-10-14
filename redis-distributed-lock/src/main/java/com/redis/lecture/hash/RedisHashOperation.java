package com.redis.lecture.hash;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class RedisHashOperation {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    public boolean hsetWithExpire(String key, String filed, String value, TimeUnit timeUnit, long expire) {

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        String scriptPath = "lua/RedisHsetWithExpire.lua";
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(scriptPath)));
        redisScript.setResultType(Long.class);
        List<String> keys = new ArrayList<>();
        keys.add(key);
        keys.add(filed);

        Long result = redisTemplate.execute(redisScript, keys, value, String.valueOf(timeUnit.toSeconds(expire)));
        return true;
    }


    /**
     *
     * @param key
     * @param fieldValue 不能太多,过多 lua unpack 命令会保存
     * @param timeUnit
     * @param expire
     * @return
     */
    public boolean hmsetWithExpire(String key, Map<String, String> fieldValue, TimeUnit timeUnit, long expire) {
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        String scriptPath = "lua/RedisHMsetWithExpire.lua";
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(scriptPath)));
        redisScript.setResultType(String.class);
        List<String> keys = new ArrayList<>();
        keys.add(key);
        String result = redisTemplate.execute(redisScript, keys, com.mybatis.utils.JacksonJsonUtils.toJson(fieldValue), String.valueOf(timeUnit.toSeconds(expire)));
        return StringUtils.equalsIgnoreCase("OK", result);
    }
}
