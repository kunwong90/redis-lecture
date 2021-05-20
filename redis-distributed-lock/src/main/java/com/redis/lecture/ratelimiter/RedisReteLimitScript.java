package com.redis.lecture.ratelimiter;

import org.springframework.data.redis.core.script.DigestUtils;
import org.springframework.data.redis.core.script.RedisScript;

public class RedisReteLimitScript implements RedisScript<String> {

    private static final String REDIS_LUA_SCRIPT = "local ratelimit_info = redis.pcall('HMGET',KEYS[1],'last_time','current_token');" +
            "local last_time = 0;" +
            "local current_token = 0;" +
            "if (ratelimit_info[1] ~= false) then" +
            " last_time = tonumber(ratelimit_info[1]);" +
            "end;" +
            "if (ratelimit_info[2] ~= false) then" +
            " current_token = tonumber(ratelimit_info[2]);" +
            "end;" +
            "redis.log(redis.LOG_NOTICE, 'last_time = '..last_time..',current_token = '..current_token);" +
            "local max_token = tonumber(ARGV[1]);" +
            "local token_rate = tonumber(ARGV[2]);" +
            "local current_time = tonumber(ARGV[3]);" +
            "local reverse_time = 1000 / token_rate;" +
            "if (current_token == 0) then" +
            "  current_token = max_token;" +
            "  last_time = current_time;" +
            "else" +
            "  local past_time = current_time-last_time" +
            "  local reverse_token = math.floor(past_time/reverse_time)" +
            "  current_token = current_token+reverse_token" +
            "  last_time = reverse_time*reverse_token+last_time" +
            "  if (current_token > max_token) then " +
            "    current_token = max_token;" +
            "  end;" +
            "end;" +
            "local result = 0;" +
            "if (current_token > 0) then " +
            "  result = 1;" +
            "  current_token = current_token - 1;" +
            "end;" +
            "redis.log(redis.LOG_NOTICE, 'result = '..result)" +
            //"redis.call('HMSET',KEYS[1],'last_time',last_time,'current_token',current_token);"+
            //"redis.log(redis.LOG_NOTICE, 'expire time = '..math.ceil(reverse_time*(max_token-current_token)+(current_time-last_time)));" +
            //"redis.call('pexpire',KEYS[1],math.ceil(reverse_time*(max_token-current_token)+(current_time-last_time)));" +
            "return result;";

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
