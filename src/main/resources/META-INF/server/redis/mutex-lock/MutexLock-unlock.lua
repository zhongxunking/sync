-- KEYS: lockKey
-- ARGV: lockerId syncChannel
-- return: true（成功）；false（失败，锁不存在或已经易主）

-- 数据结构
-- lockKey:
--   owner: lockerId

local lockKey = KEYS[1];
local lockerId = ARGV[1];
local syncChannel = ARGV[2];
-- 如果不持有锁，则无需解锁
local owner = redis.call('hget', lockKey, 'owner');
if (owner ~= lockerId) then
    if (owner == false) then
        -- 发布同步消息
        redis.call('publish', syncChannel, 0);
    end
    return false;
end
-- 解锁
redis.call('del', lockKey);
-- 发布同步消息
redis.call('publish', syncChannel, 0);
return true;
