-- KEYS: lockKey
-- ARGV: lockerId, liveTime
-- return: nil（加锁成功）；waitTime（加锁失败，需等待的毫秒时间）

-- 数据结构（hash）
-- ${lockKey}:
--   owner: ${lockerId}

local lockKey = KEYS[1];
local lockerId = ARGV[1];
local liveTime = tonumber(ARGV[2]);
-- 尝试加锁
local owner = redis.call('hget', lockKey, 'owner');
if (owner == false) then
    owner = lockerId;
    redis.call('hset', lockKey, 'owner', owner);
end
-- 保证锁关联了有效期（安全措施）
local ttl = tonumber(redis.call('pttl', lockKey));
if (ttl == -1 or ttl > liveTime) then
    ttl = liveTime;
    redis.call('pexpire', lockKey, ttl);
end
-- 计算等待时间
local waitTime = ttl;
if (owner == lockerId) then
    waitTime = nil;
end
-- 如果加锁成功，需保证锁的有效期
if (waitTime == nil) then
    if (ttl ~= liveTime) then
        ttl = liveTime;
        redis.call('pexpire', lockKey, ttl);
    end
end
return waitTime;
