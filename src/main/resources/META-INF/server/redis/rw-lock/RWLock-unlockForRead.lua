-- KEYS: lockKey
-- ARGV: readerId（不能包含'|'） messageChannel currentTime
-- return: true（正常解锁）；false（非正常解锁）

-- 数据结构
-- lockKey:
--   owner: none、reader、writer、reader-writer
--   writerBooking: 写者预定截止时间
--   writer: writerId
--   readers: |readerId1||readerId2||readerId3|

local lockKey = KEYS[1];
local readerId = ARGV[1];
local messageChannel = ARGV[2];
local currentTime = tonumber(ARGV[3]);
-- 如果不持有锁，则无需解锁
local owner = redis.call('hget', lockKey, 'owner');
if (owner ~= 'reader' and owner ~= 'reader-writer') then
    redis.call('publish', messageChannel, 0);
    return false;
end
-- 删除当前读者。如果还存在其他读者，则无需解锁
local readers = redis.call('hget', lockKey, 'readers');
if (readers == false) then
    readers = '';
end
readers = string.gsub(readers, '|' .. readerId .. '|', '');
if (readers ~= '') then
    redis.call('hset', lockKey, 'readers', readers);
    -- 发布解锁消息
    redis.call('publish', messageChannel, 0);
    return true;
end
redis.call('hdel', lockKey, 'readers');
-- 如果存在当前写者，则无需解锁
if (owner == 'reader-writer') then
    owner = 'writer';
    redis.call('hset', lockKey, 'owner', owner);
    return true;
end
-- 解锁
-- 如果其他写者已预定且预定有效，则保留预订记录（防止写者被饿死）；否则删除锁记录
local writerBooking = redis.call('hget', lockKey, 'writerBooking');
if (writerBooking ~= false) then
    writerBooking = tonumber(writerBooking);
    if (writerBooking >= currentTime) then
        owner = 'none';
        redis.call('hset', lockKey, 'owner', owner);
    else
        redis.call('del', lockKey);
    end
else
    redis.call('del', lockKey);
end
-- 解锁成功，发布解锁消息
redis.call('publish', messageChannel, 0);
return true;
