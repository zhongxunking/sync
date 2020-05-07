-- KEYS: lockKey
-- ARGV: writerId（不能包含'|'） defaultExpire writerBooking
-- return: nil（加锁成功）；waitingTime（加锁失败，需等待的时间）

-- 数据结构
-- lockKey:
--   owner: none、reader、writer、reader-writer
--   writerBooking: 写者预定截止时间
--   writer: writerId
--   readers: |readerId1||readerId2||readerId3|

local lockKey = KEYS[1];
local writerId = ARGV[1];
local defaultExpire = tonumber(ARGV[2]);
local writerBooking = tonumber(ARGV[3]);
-- 如果锁未被占用则抢占锁
local owner = redis.call('hget', lockKey, 'owner');
if (owner == false or owner == 'none') then
    owner = 'writer';
    redis.call('hset', lockKey, 'owner', owner);
    redis.call('hset', lockKey, 'writer', writerId);
end
-- 保证锁关联了有效期（安全措施）
local ttl = tonumber(redis.call('pttl', lockKey));
if (ttl == -1 or ttl > defaultExpire) then
    ttl = defaultExpire;
    redis.call('pexpire', lockKey, ttl);
end
-- 尝试加锁
if (owner == 'reader') then
    -- 如果持有锁的读者不只一个或不是当前写者，则加锁失败且预定锁
    local readers = redis.call('hget', lockKey, 'readers');
    if (readers ~= '|' .. writerId .. '|') then
        redis.call('hset', lockKey, 'writerBooking', writerBooking);
        return math.max(ttl, 0);
    end
    -- 加锁
    owner = 'reader-writer';
    redis.call('hset', lockKey, 'owner', owner);
    redis.call('hset', lockKey, 'writer', writerId);
else
    -- 如果持有锁的写者和当前写者不是同一个，则加锁失败
    local writer = redis.call('hget', lockKey, 'writer');
    if (writer ~= writerId) then
        return math.max(ttl, 0);
    end
end
-- 加锁成功，清理锁预定记录
redis.call('hdel', lockKey, 'writerBooking');
-- 保证锁的有效期
if (ttl ~= defaultExpire) then
    ttl = defaultExpire;
    redis.call('pexpire', lockKey, ttl);
end
return nil;
