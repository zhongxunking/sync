-- KEYS: lockKey
-- ARGV: writerId（不能包含'|'） messageChannel
-- return: true（正常解锁）；false（非正常解锁）

-- 数据结构
-- lockKey:
--   owner: none、reader、writer、reader-writer
--   writerBooking: 写者预定截止时间
--   writer: writerId
--   readers: |readerId1||readerId2||readerId3|

local lockKey = KEYS[1];
local writerId = ARGV[1];
local messageChannel = ARGV[2];
-- 如果不持有锁，则无需解锁
local owner = redis.call('hget', lockKey, 'owner');
if (owner ~= 'writer' and owner ~= 'reader-writer') then
    redis.call('publish', messageChannel, 0);
    return false;
end
local writer = redis.call('hget', lockKey, 'writer');
if (writer ~= writerId) then
    return false;
end
-- 解锁
if (owner == 'writer') then
    -- 删除锁记录
    redis.call('del', lockKey);
else
    -- 如果存在当前读者，则无需解锁
    owner = 'reader';
    redis.call('hset', lockKey, 'owner', owner);
    redis.call('hdel', lockKey, 'writer');
end
-- 解锁成功，发布解锁消息
redis.call('publish', messageChannel, 0);
return true;
