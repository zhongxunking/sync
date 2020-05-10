-- KEYS: lockKey
-- ARGV: lockerId, currentTime, liveTime
-- return: nil（加锁成功）；waitTime（加锁失败，需等待的毫秒时间）

-- 数据结构（hash）
-- ${lockKey}:
--   owner: none、writer、readers、reader-writer
--   writerBooking: ${writerBooking}
--   writer: ${lockerId}
--   readerAmount: ${读者数量}
--   readerAmountDeadline: ${readerAmount的存活时间}
--   reader-${lockerId1}: ${deadline1}
--   reader-${lockerId2}: ${deadline2}
--   reader-${lockerId3}: ${deadline3}

local lockKey = KEYS[1];
local lockerId = ARGV[1];
local currentTime = tonumber(ARGV[2]);
local liveTime = tonumber(ARGV[3]);
-- 获取owner
local owner = redis.call('hget', lockKey, 'owner');
if (owner == false) then
    owner = 'none';
    redis.call('hset', lockKey, 'owner', owner);
end
-- 保证锁关联了有效期（安全措施）
local ttl = tonumber(redis.call('pttl', lockKey));
if (ttl == -1 or ttl > liveTime) then
    ttl = liveTime;
    redis.call('pexpire', lockKey, ttl);
end
-- 获取writerBooking
local writerBooking = redis.call('hget', lockKey, 'writerBooking');
if (writerBooking ~= false) then
    writerBooking = tonumber(writerBooking);
end
-- 获取readerAmount及其有效期
local readerAmount = redis.call('hget', lockKey, 'readerAmount');
if (readerAmount ~= false) then
    readerAmount = tonumber(readerAmount);
end
local readerAmountDeadline = redis.call('hget', lockKey, 'readerAmountDeadline');
if (readerAmountDeadline ~= false) then
    readerAmountDeadline = tonumber(readerAmountDeadline);
end
-- 如果readerAmount不存在或已失效，则重新统计readerAmount
if (readerAmount == false or readerAmountDeadline == false or readerAmountDeadline < currentTime) then
    readerAmount = 0;
    readerAmountDeadline = currentTime + liveTime;
    -- 遍历所有key
    local keys = redis.call('hkeys', lockKey);
    for i = 1, #keys do
        local key = keys[i]
        -- 如果key以'reader-'开头，则该key代表一个读者
        local index = string.find(key, 'reader-', 1, true);
        if (index == 1) then
            -- 获取key对应的value
            local value = redis.call('hget', lockKey, key);
            if (value ~= false) then
                local deadline = tonumber(value);
                if (deadline >= currentTime) then
                    readerAmount = readerAmount + 1;
                    readerAmountDeadline = math.min(readerAmountDeadline, deadline);
                else
                    redis.call('hdel', lockKey, key);
                end
            end
        end
    end
    -- 更新readerAmount及其效期
    redis.call('hset', lockKey, 'readerAmount', readerAmount);
    redis.call('hset', lockKey, 'readerAmountDeadline', readerAmountDeadline);
end
-- 尝试加读锁
local waitTime = ttl;
if (owner == 'none' or owner == 'readers') then
    -- 如果writer未预订或预订无效，则加读锁
    if (writerBooking == false or writerBooking < currentTime) then
        -- 更新owner
        owner = 'readers';
        redis.call('hset', lockKey, 'owner', owner);
        -- 添加reader
        local readerKey = 'reader-' .. lockerId;
        local deadline = redis.call('hget', lockKey, readerKey);
        if (deadline == false) then
            readerAmount = readerAmount + 1;
            redis.call('hset', lockKey, 'readerAmount', readerAmount);
        end
        deadline = currentTime + liveTime;
        redis.call('hset', lockKey, readerKey, deadline);
        -- 删除writerBooking
        redis.call('hdel', lockKey, 'writerBooking');

        waitTime = nil;
    end
else
    local writer = redis.call('hget', lockKey, 'writer');
    if (lockerId == writer) then
        -- 更新owner
        owner = 'reader-writer';
        redis.call('hset', lockKey, 'owner', owner);
        -- 添加reader
        local readerKey = 'reader-' .. lockerId;
        local deadline = redis.call('hget', lockKey, readerKey);
        if (deadline == false) then
            readerAmount = readerAmount + 1;
            redis.call('hset', lockKey, 'readerAmount', readerAmount);
        end
        deadline = currentTime + liveTime;
        redis.call('hset', lockKey, readerKey, deadline);

        waitTime = nil;
    end
end
if (waitTime == nil) then
    -- 加锁成功，保证锁的有效期
    if (ttl ~= liveTime) then
        ttl = liveTime;
        redis.call('pexpire', lockKey, ttl);
    end
end
return waitTime;
