local key = KEYS[1];
local field = KEYS[2];
local value = ARGV[1];
local result = redis.pcall('HSET', key, field, value);
local expire = tonumber(ARGV[2]);
if (expire > 0) then
    redis.pcall('expire', key, expire);
end
redis.log(redis.LOG_NOTICE, 'RedisHsetWithExpire result = ' .. result);
return tonumber(result);