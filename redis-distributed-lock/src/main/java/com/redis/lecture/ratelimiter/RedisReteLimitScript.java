package com.redis.lecture.ratelimiter;

import com.google.common.io.Resources;
import org.springframework.data.redis.core.script.DigestUtils;
import org.springframework.data.redis.core.script.RedisScript;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RedisReteLimitScript implements RedisScript<String> {

    private static String REDIS_LUA_SCRIPT;

    static {
        try {
            REDIS_LUA_SCRIPT = Resources.toString(Resources.getResource("lua/RedisReteLimitScript.lua"), StandardCharsets.UTF_8);
        } catch (IOException ignore) {

        }

    }

    @Override
    public String getSha1() {
        return DigestUtils.sha1DigestAsHex(REDIS_LUA_SCRIPT);
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    @Override
    public String getScriptAsString() {
        return REDIS_LUA_SCRIPT;
    }

    @Override
    public boolean returnsRawValue() {
        return RedisScript.super.returnsRawValue();
    }
}
