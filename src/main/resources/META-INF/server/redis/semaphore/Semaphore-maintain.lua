-- KEYS: semaphoreKey
-- ARGV: semaphoreId currentTime activeTimeout
-- return: true（成功）；false（失败）

-- 数据结构
-- ${semaphoreKey}:
--   allPermits: ${所有semaphore的permits之和}
--   allPermitsDeadline: ${permits的存活时间}
--   semaphore-${semaphoreId}: ${permits}|${deadline}

local semaphoreKey = KEYS[1];
local semaphoreId = ARGV[1];
local currentTime = tonumber(ARGV[2]);
local activeTimeout = tonumber(ARGV[3]);
-- 获取客户端已得到的许可数
local permits = 0;
local value = redis.call('hget', semaphoreKey, 'semaphore-' .. semaphoreId);
if (value ~= false) then
    local separatorIndex = string.find(value, '|', 1, true);
    if (separatorIndex ~= nil) then
        permits = tonumber(string.sub(value, 1, separatorIndex - 1));
    end
end
-- 如果客户端不持有许可，则无需再维护
if (permits <= 0) then
    return false;
end
-- 更新客户端持有许可的有效期
local deadline = currentTime + activeTimeout;
redis.call('hset', semaphoreKey, 'semaphore-' .. semaphoreId, permits .. '|' .. deadline);
-- 更新信号量的有效期
redis.call('pexpire', semaphoreKey, activeTimeout);
return true;
