-- KEYS: semaphoreKey
-- ARGV: semaphorerId, newPermits, totalPermits, currentTime, syncChannel, liveTime, force
-- return: nil（成功）；waitingTime（失败，需等待的时间）

-- 数据结构
-- ${semaphoreKey}:
--   allPermits: ${所有permits}
--   allPermitsDeadline: ${allPermits的存活时间}
--   semaphorer-${semaphorerId1}: ${permits1}|${deadline1}
--   semaphorer-${semaphorerId2}: ${permits2}|${deadline2}
--   semaphorer-${semaphorerId3}: ${permits3}|${deadline3}

local semaphoreKey = KEYS[1];
local semaphorerId = ARGV[1];
local newPermits = tonumber(ARGV[2]);
local totalPermits = tonumber(ARGV[3]);
local currentTime = tonumber(ARGV[4]);
local syncChannel = ARGV[5];
local liveTime = tonumber(ARGV[6]);
local force = ARGV[7] == 'true';
-- 获取allPermits及其有效期
local allPermits = redis.call('hget', semaphoreKey, 'allPermits');
if (allPermits ~= false) then
    allPermits = tonumber(allPermits);
end
local allPermitsDeadline = redis.call('hget', semaphoreKey, 'allPermitsDeadline');
if (allPermitsDeadline ~= false) then
    allPermitsDeadline = tonumber(allPermitsDeadline);
end
-- 如果allPermits不存在或已失效，则重新统计allPermits
if (allPermits == false or allPermitsDeadline == false or allPermitsDeadline < currentTime) then
    allPermits = 0;
    allPermitsDeadline = currentTime + liveTime;
    -- 遍历所有key
    local keys = redis.call('hkeys', semaphoreKey);
    for i = 1, #keys do
        local key = keys[i]
        -- 如果key以'semaphorer-'开头，则该key代表一个semaphorer
        local index = string.find(key, 'semaphorer-', 1, true);
        if (index == 1) then
            -- 获取key对应的value
            local value = redis.call('hget', semaphoreKey, key);
            if (value ~= false) then
                -- 解析出permits、deadline
                local separatorIndex = string.find(value, '|', 1, true);
                local permits = tonumber(string.sub(value, 1, separatorIndex - 1));
                local deadline = tonumber(string.sub(value, separatorIndex + 1));
                if (deadline >= currentTime) then
                    allPermits = allPermits + permits;
                    allPermitsDeadline = math.min(allPermitsDeadline, deadline);
                else
                    redis.call('hdel', semaphoreKey, key);
                end
            end
        end
    end
    -- 更新allPermits及其有效期
    redis.call('hset', semaphoreKey, 'allPermits', allPermits);
    redis.call('hset', semaphoreKey, 'allPermitsDeadline', allPermitsDeadline);
end
-- 保证信号量关联了有效期（安全措施）
local ttl = tonumber(redis.call('pttl', semaphoreKey));
if (ttl == -1 or ttl > liveTime) then
    ttl = liveTime;
    redis.call('pexpire', semaphoreKey, ttl);
end
-- 获取permits
local oldPermits = 0;
local semaphorerKey = 'semaphorer-' .. semaphorerId;
local semaphorerValue = redis.call('hget', semaphoreKey, semaphorerKey);
if (semaphorerValue ~= false) then
    local separatorIndex = string.find(semaphorerValue, '|', 1, true);
    oldPermits = tonumber(string.sub(semaphorerValue, 1, separatorIndex - 1));
end
-- 尝试更新permits
local waitTime = math.min(ttl, allPermitsDeadline - currentTime);
if (force == true or newPermits <= oldPermits or allPermits + newPermits - oldPermits <= totalPermits) then
    -- 更新semahorer
    if (newPermits > 0) then
        local deadline = currentTime + liveTime;
        semaphorerValue = newPermits .. '|' .. deadline;
        redis.call('hset', semaphoreKey, semaphorerKey, semaphorerValue);
    else
        redis.call('hdel', semaphoreKey, semaphorerKey);
    end
    -- 更新allPermits
    allPermits = allPermits + newPermits - oldPermits
    redis.call('hset', semaphoreKey, 'allPermits', allPermits);

    waitTime = nil;
end
-- 如果更新permits成功，需保证信号量的有效期
if (waitTime == nil) then
    if (ttl ~= liveTime) then
        ttl = liveTime;
        redis.call('pexpire', semaphoreKey, ttl);
    end
end
-- 如果为释放许可，则发送同步消息
if (newPermits < oldPermits) then
    redis.call('publish', syncChannel, 0);
end
return waitTime;
