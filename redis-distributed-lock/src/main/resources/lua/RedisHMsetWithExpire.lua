local key = KEYS[1];
local kv = cjson.decode(ARGV[1]);
local payload = {};
local index = 0;
for k, v in pairs(kv) do
    if (index < 500) then
        table.insert(payload, k);
        table.insert(payload, v);
        index = index + 1;
    else
        redis.pcall('hmset', key, unpack(payload));
        payload = {};
        table.insert(payload, k);
        table.insert(payload, v);
        index = 1;
    end
end
if (index > 0) then
    redis.pcall('hmset', key, unpack(payload));
end
local expire = tonumber(ARGV[2]);
if (expire > 0) then
    redis.pcall('expire', key, expire);
end
return 'OK';