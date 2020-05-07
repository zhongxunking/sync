-- KEYS: semaphoreKey
-- ARGV: semaphoreId totalPermits newPermits forceSuccess currentTime activeTimeout messageChannel
-- return: nil（成功）；waitingTime（失败，需等待的时间）

-- 数据结构
-- ${semaphoreKey}:
--   allPermits: ${所有semaphore的permits之和}
--   allPermitsDeadline: ${permits的存活时间}
--   semaphore-${semaphoreId}: ${permits}|${deadline}

local semaphoreKey = KEYS[1];
local semaphoreId = ARGV[1];
local totalPermits = tonumber(ARGV[2]);
local newPermits = tonumber(ARGV[3]);
local forceSuccess = ARGV[4] == 'true';
local currentTime = tonumber(ARGV[5]);
local activeTimeout = tonumber(ARGV[6]);
local messageChannel = ARGV[7];
-- 获取所有许可数及有效期
local allPermits = redis.call('hget', semaphoreKey, 'allPermits');
if (allPermits ~= false) then
    allPermits = tonumber(allPermits);
end
local allPermitsDeadline = redis.call('hget', semaphoreKey, 'allPermitsDeadline');
if (allPermitsDeadline ~= false) then
    allPermitsDeadline = tonumber(allPermitsDeadline);
end
-- 如果所有许可数不存在或已失效，则重新统计所有许可数
if (allPermits == false or allPermitsDeadline == false or allPermitsDeadline < currentTime) then
    allPermits = 0;
    allPermitsDeadline = currentTime + activeTimeout;
    -- 遍历所有key
    local keys = redis.call('hkeys', semaphoreKey);
    for i = 1, #keys do
        -- 如果key以'semaphore-'开头，则该key代表一个客户端获取的许可
        local index = string.find(keys[i], 'semaphore-', 1, true);
        if (index == 1) then
            -- 获取key对应的value
            local value = redis.call('hget', semaphoreKey, keys[i]);
            if (value ~= false) then
                -- 如果value包含'|'，则进一步处理；否则删除该key
                local separatorIndex = string.find(value, '|', 1, true);
                if (separatorIndex ~= nil) then
                    -- 得到许可数及有效期
                    local permits = tonumber(string.sub(value, 1, separatorIndex - 1));
                    local deadline = tonumber(string.sub(value, separatorIndex + 1));
                    -- 如果客户端的许可有效，则加入统计；否则删除客户端获取的许可
                    if (deadline >= currentTime) then
                        allPermits = allPermits + permits;
                        allPermitsDeadline = math.min(allPermitsDeadline, deadline);
                    else
                        redis.call('hdel', semaphoreKey, keys[i]);
                    end
                else
                    redis.call('hdel', semaphoreKey, keys[i]);
                end
            end
        end
    end
    -- 更新所有许可数及有效期
    redis.call('hset', semaphoreKey, 'allPermits', allPermits);
    redis.call('hset', semaphoreKey, 'allPermitsDeadline', allPermitsDeadline);
end
-- 保证信号量关联了有效期（安全措施）
local ttl = tonumber(redis.call('pttl', semaphoreKey));
if (ttl == -1 or ttl > activeTimeout) then
    ttl = activeTimeout;
    redis.call('pexpire', semaphoreKey, ttl);
end
-- 获取客户端已得到的许可数
local permits = 0;
local value = redis.call('hget', semaphoreKey, 'semaphore-' .. semaphoreId);
if (value ~= false) then
    local separatorIndex = string.find(value, '|', 1, true);
    if (separatorIndex ~= nil) then
        permits = tonumber(string.sub(value, 1, separatorIndex - 1));
    end
end
-- 如果非强制成功且可用许可不满足本次获取的许可数，则获取许可失败
if (forceSuccess == false and newPermits > permits and allPermits + newPermits - permits > totalPermits) then
    return math.max(math.min(allPermitsDeadline - currentTime, ttl), 0);
end
-- 如果客户端还持有许可，则更新客户端的许可数及有效期；否则删除
if (newPermits > 0) then
    local deadline = currentTime + activeTimeout;
    redis.call('hset', semaphoreKey, 'semaphore-' .. semaphoreId, newPermits .. '|' .. deadline);
else
    redis.call('hdel', semaphoreKey, 'semaphore-' .. semaphoreId);
end
-- 更新所有许可数
allPermits = allPermits + newPermits - permits;
redis.call('hset', semaphoreKey, 'allPermits', allPermits);
-- 更新许可成功，保证信号量的有效期
if (ttl ~= activeTimeout) then
    ttl = activeTimeout;
    redis.call('pexpire', semaphoreKey, ttl);
end
-- 如果为释放许可，则发送消息
if (newPermits < permits) then
    redis.call('publish', messageChannel, 0);
end
return nil;
