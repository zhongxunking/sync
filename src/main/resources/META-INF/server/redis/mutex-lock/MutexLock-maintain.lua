-- KEYS: lockKey
-- ARGV: lockerId, liveTime
-- return: true（成功）；false（失败，锁不存在或已经易主）

-- 数据结构（hash）
-- ${lockKey}:
--   owner: ${lockerId}

local lockKey = KEYS[1];
local lockerId = ARGV[1];
local liveTime = tonumber(ARGV[2]);
-- 尝试维护
local alive = false;
local owner = redis.call('hget', lockKey, 'owner');
if (owner == lockerId) then
    -- 维护
    redis.call('pexpire', lockKey, liveTime);
    alive = true;
end
return alive;
