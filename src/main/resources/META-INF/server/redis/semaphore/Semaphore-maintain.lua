-- KEYS: semaphoreKey
-- ARGV: semaphorerId, currentTime, liveTime
-- return: true（成功）；false（失败，信号量不存在或已经不持有许可）

-- 数据结构（hash）
-- ${semaphoreKey}:
--   allPermits: ${所有permits}
--   allPermitsDeadline: ${allPermits的存活时间}
--   semaphorer-${semaphorerId1}: ${permits1}|${deadline1}
--   semaphorer-${semaphorerId2}: ${permits2}|${deadline2}
--   semaphorer-${semaphorerId3}: ${permits3}|${deadline3}

local semaphoreKey = KEYS[1];
local semaphorerId = ARGV[1];
local currentTime = tonumber(ARGV[2]);
local liveTime = tonumber(ARGV[3]);
-- 尝试维护
local alive = false;
local semaphorerKey = 'semaphorer-' .. semaphorerId;
local semaphorerValue = redis.call('hget', semaphoreKey, semaphorerKey);
if (semaphorerValue ~= false) then
    -- 解析permits
    local separatorIndex = string.find(semaphorerValue, '|', 1, true);
    local permits = tonumber(string.sub(semaphorerValue, 1, separatorIndex - 1));
    -- 维护semaphorer
    local deadline = currentTime + liveTime;
    semaphorerValue = permits .. '|' .. deadline;
    redis.call('hset', semaphoreKey, semaphorerKey, semaphorerValue);
    -- 维护信号量
    redis.call('pexpire', semaphoreKey, liveTime);

    alive = true;
end
return alive;
