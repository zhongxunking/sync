-- KEYS: lockKey
-- ARGV: lockerId, syncChannel
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
local syncChannel = ARGV[2];
-- 获取owner
local owner = redis.call('hget', lockKey, 'owner');
if (owner == false) then
    return false;
end
-- 尝试解读锁
local success = false;
if (owner == 'writer' or owner == 'reader-writer') then
    -- 获取writer
    local writer = redis.call('hget', lockKey, 'writer');
    if (lockerId == writer) then
        -- 删除writer
        writer = false;
        redis.call('hdel', lockKey, 'writer');
        if (owner == 'writer') then
            -- 删除锁
            redis.call('del', lockKey);
        else
            -- 更新owner
            owner = 'readers';
            redis.call('hset', lockKey, 'owner', owner);
        end
        success = true;
    end
end
-- 发布同步消息
redis.call('publish', syncChannel, 0);
return success;
