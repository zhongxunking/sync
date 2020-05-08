-- KEYS: lockKey
-- ARGV: lockerId（不能包含'|'） defaultExpire
-- return: nil（加锁成功）；waitingTime（加锁失败，需等待的时间）

-- 数据结构
-- lockKey:
--   owner: lockerId

local lockKey = KEYS[1];
local lockerId = ARGV[1];
local defaultExpire = tonumber(ARGV[2]);
-- 如果锁不存在，则抢占锁
local owner = redis.call('hget', lockKey, 'owner');
if (owner == false) then
    owner = lockerId;
    redis.call('hset', lockKey, 'owner', owner);
end
-- 保证锁关联了有效期（安全措施）
local ttl = tonumber(redis.call('pttl', lockKey));
if (ttl == -1 or ttl > defaultExpire) then
    ttl = defaultExpire;
    redis.call('pexpire', lockKey, ttl);
end
-- 如果锁已被其他locker占有，则加锁失败
if (owner ~= lockerId) then
    return math.max(ttl, 0);
end
-- 加锁成功，保证锁的有效期
if (ttl ~= defaultExpire) then
    ttl = defaultExpire;
    redis.call('pexpire', lockKey, ttl);
end
return nil;
