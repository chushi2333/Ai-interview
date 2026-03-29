local key_prefix = KEYS[1]
local phone = ARGV[1]
local ip = ARGV[2]
local now = tonumber(ARGV[3])

-- 1. 检查手机号是否被冻结
local phone_freeze_key = key_prefix .. ':freeze:phone:' .. phone
local is_phone_frozen = redis.call('EXISTS', phone_freeze_key)
if is_phone_frozen ~= 0 then
    return 'PHONE_FROZEN'
end

-- 2. 检查 IP 是否被冻结
local ip_freeze_key = key_prefix .. ':freeze:ip:' .. ip
local is_ip_frozen = redis.call('EXISTS', ip_freeze_key)
if is_ip_frozen ~= 0 then
    return 'IP_FROZEN'
end

-- 定义时间相关常量
local MS_01M = 60 * 1000      --  1 分钟
local MS_10M = 30 * 60 * 1000 -- 10 分钟

-- 拼接 KEY
local phone_limit_key = key_prefix .. ':limit:phone:' .. phone
local ip_limit_key = key_prefix .. ':limit:ip:' .. ip

-- 3. 清除十分钟前的记录
redis.call('ZREMRANGEBYSCORE', phone_limit_key, 0, now - MS_10M)
redis.call('ZREMRANGEBYSCORE', ip_limit_key, 0, now - MS_10M)

-- 4. 对手机号进行限流策略
--    1 分钟内有记录 - 禁止重新发送
--    10 分钟内有 9 条及以上记录 - 冻结 1 天
-- TODO: 优化限流策略
local phone_count_1m = redis.call('ZCOUNT', phone_limit_key, now - MS_01M, now)
if phone_count_1m > 0 then
    return 'TOO_FREQUENT_PHONE_1M'
end

local phone_freeze_duration = 0
local phone_freeze_reason = ''

if redis.call('ZCOUNT', phone_limit_key, now - MS_10M, now) >= 9 then
    -- 10 分钟内 9 次以上请求
    phone_freeze_duration = 24 * 60 * 60 -- 1d
    phone_freeze_reason = '1'
--elseif redis.call('ZCOUNT', phone_limit_key, now - MS_05M, now) >= 4 then
--    phone_freeze_duration = 10 * 60 -- 10m
--    phone_freeze_reason = '2'
end

-- 添加当前请求记录
redis.call('ZADD', phone_limit_key, now, now)
redis.call('EXPIRE', phone_limit_key, 60 * 10 + 1) -- 10分钟过期兜底

-- 进行冻结
if phone_freeze_duration ~= 0 then
    redis.call('SET', phone_freeze_key, phone_freeze_reason, 'EX', phone_freeze_duration)
    return 'PHONE_FROZEN'
end

-- 5. TODO: 对 IP 进行限流的策略

-- 允许发送
return 'OK'
