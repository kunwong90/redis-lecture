redis.replicate_commands();
local ratelimit_info = redis.pcall('HMGET', KEYS[1], 'last_time', 'current_token');
local last_time = tonumber(ratelimit_info[1]);
local current_token = tonumber(ratelimit_info[2]);
local max_token = tonumber(ARGV[1]);
local token_rate = tonumber(ARGV[2]);
local time = redis.call('time');
--local current_time = tonumber(ARGV[3]);
local current_time = tonumber(time[1]*1000+math.ceil(time[2]/1000));
local reverse_time = 1000 / token_rate;
if (current_token == nil) then
    current_token = max_token;
    last_time = current_time;
else
    local past_time = current_time - last_time;
    -- 需要放进桶的令牌数量
    local reverse_token = math.floor(past_time / reverse_time);
    current_token = current_token + reverse_token;
    last_time = reverse_time * reverse_token + last_time;
    if (current_token > max_token) then
        current_token = max_token;
    end ;
end ;
local result = 0;
redis.log(redis.LOG_NOTICE, 'current_token = ' .. current_token);
if (current_token > 0) then
    result = 1;
    current_token = current_token - 1;
end ;
redis.log(redis.LOG_NOTICE, 'result = ' .. result);
redis.call('HMSET', KEYS[1], 'last_time', last_time, 'current_token', current_token);
redis.log(redis.LOG_NOTICE, 'expire time = ' .. math.ceil(reverse_time * (max_token - current_token) + (current_time - last_time)));
redis.call('pexpire', KEYS[1], math.ceil(reverse_time * (max_token - current_token) + (current_time - last_time)));
--直接返回reurn result报错,Redis exception; nested exception is io.lettuce.core.RedisException: java.lang.IllegalStateException
return tostring(result);