-- KEYS: lockKey
-- ARGV: lockerId（不能包含'|'） messageChannel
-- return: true（正常解锁）；false（非正常解锁）

-- 数据结构
-- lockKey:
--   owner: lockerId

local lockKey = KEYS[1];
local lockerId = ARGV[1];
local messageChannel = ARGV[2];
-- 如果不持有锁，则无需解锁
local owner = redis.call('hget', lockKey, 'owner');
if (owner ~= lockerId) then
    if (owner == false) then
        redis.call('publish', messageChannel, 0);
    end
    return false;
end
-- 解锁
redis.call('del', lockKey);
-- 解锁成功，发布解锁消息
redis.call('publish', messageChannel, 0);
return true;
