-- KEYS: lockKey
-- ARGV: lockerId, syncChannel
-- return: true（成功）；false（失败，锁不存在或已经易主）

-- 数据结构（hash）
-- ${lockKey}:
--   owner: ${lockerId}

local lockKey = KEYS[1];
local lockerId = ARGV[1];
local syncChannel = ARGV[2];
-- 尝试解锁
local success = false;
local owner = redis.call('hget', lockKey, 'owner');
if (owner == lockerId) then
    -- 解锁
    redis.call('del', lockKey);
    success = true;
end
-- 发布同步消息
redis.call('publish', syncChannel, 0);
return success;
