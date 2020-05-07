-- KEYS: lockKey
-- ARGV: readerId（不能包含'|'） defaultExpire currentTime
-- return: nil（加锁成功）；waitingTime（加锁失败，需等待的时间）

-- 数据结构
-- lockKey:
--   owner: none、reader、writer、reader-writer
--   writerBooking: 写者预定截止时间
--   writer: writerId
--   readers: |readerId1||readerId2||readerId3|

local lockKey = KEYS[1];
local readerId = ARGV[1];
local defaultExpire = tonumber(ARGV[2]);
local currentTime = tonumber(ARGV[3]);
-- 如果锁不存在则抢占锁
local owner = redis.call('hget', lockKey, 'owner');
if (owner == false) then
    owner = 'reader';
    redis.call('hset', lockKey, 'owner', owner);
end
-- 保证锁关联了有效期（安全措施）
local ttl = tonumber(redis.call('pttl', lockKey));
if (ttl == -1 or ttl > defaultExpire) then
    ttl = defaultExpire;
    redis.call('pexpire', lockKey, ttl);
end
-- 尝试加锁
if (owner == 'writer' or owner == 'reader-writer') then
    -- 如果持有锁的写者和当前读者不是同一个，则加锁失败
    local writer = redis.call('hget', lockKey, 'writer');
    if (writer ~= readerId) then
        return math.max(ttl, 0);
    end
    -- 加锁
    if (owner == 'writer') then
        owner = 'reader-writer';
        redis.call('hset', lockKey, 'owner', owner);
        redis.call('hset', lockKey, 'readers', '|' .. readerId .. '|');
    end
else
    -- 如果锁已经被写者预订且预订有效，则加锁失败（防止写者被饿死）
    local writerBooking = redis.call('hget', lockKey, 'writerBooking');
    if (writerBooking ~= false) then
        writerBooking = tonumber(writerBooking);
        if (writerBooking > currentTime) then
            return math.max(math.min(ttl, writerBooking - currentTime), 0);
        end
        redis.call('hdel', lockKey, 'writerBooking');
    end
    -- 加锁
    if (owner == 'none') then
        owner = 'reader';
        redis.call('hset', lockKey, 'owner', owner);
    end
    local readers = redis.call('hget', lockKey, 'readers');
    if (readers == false) then
        readers = '';
    end
    if (string.find(readers, '|' .. readerId .. '|', 1, true) == nil) then
        readers = readers .. '|' .. readerId .. '|';
        redis.call('hset', lockKey, 'readers', readers);
    end
end
-- 加锁成功，保证锁的有效期
if (ttl ~= defaultExpire) then
    ttl = defaultExpire;
    redis.call('pexpire', lockKey, ttl);
end
return nil;
