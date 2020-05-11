-- KEYS: lockKey
-- ARGV: lockerId, deadline, currentTime, liveTime
-- return: nil（加锁成功）；waitTime（加锁失败，需等待的毫秒时间）

-- 数据结构（hash）
-- ${lockKey}:
--   owner: none、writer、readers、reader-writer
--   writerBooking: ${writerBooking}
--   writer: ${lockerId}
--   readerAmount: ${读者数量}
--   readerAmountDeadline: ${readerAmount的存活时间}
--   reader-${lockerId1}: ${readerDeadline1}
--   reader-${lockerId2}: ${readerDeadline2}
--   reader-${lockerId3}: ${readerDeadline3}

local lockKey = KEYS[1];
local lockerId = ARGV[1];
local deadline = tonumber(ARGV[2]);
local currentTime = tonumber(ARGV[3]);
local liveTime = tonumber(ARGV[4]);
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
                local readerDeadline = tonumber(value);
                if (readerDeadline >= currentTime) then
                    readerAmount = readerAmount + 1;
                    readerAmountDeadline = math.min(readerAmountDeadline, readerDeadline);
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
if (readerAmount <= 0) then
    if (owner == 'readers') then
        owner = 'none';
        redis.call('hset', lockKey, 'owner', owner);
    elseif (owner == 'reader-writer') then
        owner = 'writer';
        redis.call('hset', lockKey, 'owner', owner);
    end
end
-- 尝试加写锁
if (owner == 'none') then
    -- 更新owner
    owner = 'writer';
    redis.call('hset', lockKey, 'owner', owner);
    -- 更新writer
    redis.call('hset', lockKey, 'writer', lockerId);
    -- 删除writerBooking
    redis.call('hdel', lockKey, 'writerBooking');
elseif (owner == 'readers') then
    local readerKey = 'reader-' .. lockerId;
    local readerDeadline = redis.call('hget', lockKey, readerKey);
    if (readerAmount == 1 and readerDeadline ~= false) then
        -- 更新owner
        owner = 'reader-writer';
        redis.call('hset', lockKey, 'owner', owner);
        -- 更新writer
        redis.call('hset', lockKey, 'writer', lockerId);
        -- 删除writerBooking
        redis.call('hdel', lockKey, 'writerBooking');
    else
        -- 更新writerBooking
        redis.call('hset', lockKey, 'writerBooking', deadline);
    end
end
-- 计算等待时间
local waitTime = math.min(ttl, readerAmountDeadline - currentTime);
local writer = redis.call('hget', lockKey, 'writer');
if (writer == lockerId) then
    waitTime = nil;
end
-- 如果加锁成功，需保证锁的有效期
if (waitTime == nil) then
    if (ttl ~= liveTime) then
        ttl = liveTime;
        redis.call('pexpire', lockKey, ttl);
    end
end
return waitTime;
