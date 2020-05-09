-- KEYS: lockKey
-- ARGV: lockerId, liveTime
-- return: true（成功）；false（失败，锁不存在或已经易主）

-- 数据结构
-- lockKey:
--   owner: lockerId

local lockKey = KEYS[1];
local lockerId = ARGV[1];
local liveTime = tonumber(ARGV[2]);
-- 如果不持有锁，则无需维护
local owner = redis.call('hget', lockKey, 'owner');
if (owner ~= lockerId) then
    return false;
end
-- 设置有效期
redis.call('pexpire', lockKey, liveTime);
return true;
