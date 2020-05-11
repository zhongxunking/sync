-- KEYS: lockKey
-- ARGV: lockerId, liveTime
-- return: true（成功）；false（失败，锁不存在或已经易主）

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
local liveTime = tonumber(ARGV[2]);
-- 尝试维护
local alive = false;
local owner = redis.call('hget', lockKey, 'owner');
if (owner == 'writer' or owner == 'reader-writer') then
    local writer = redis.call('hget', lockKey, 'writer');
    if (lockerId == writer) then
        -- 维护锁
        redis.call('pexpire', lockKey, liveTime);
        alive = true;
    end
end
return alive;
