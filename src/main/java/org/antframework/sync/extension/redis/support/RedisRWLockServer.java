/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-07 21:08 创建
 */
package org.antframework.sync.extension.redis.support;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antframework.sync.common.SyncUtils;
import org.antframework.sync.extension.redis.extension.RedisExecutor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 基于redis的读写锁服务端
 */
@AllArgsConstructor
@Slf4j
public class RedisRWLockServer {
    // 加读锁脚本
    private static final String LOCK_FOR_READ_SCRIPT = SyncUtils.getScript("META-INFO/server/redis/rw-lock/ReentrantReadLock-lock.lua");
    // 解读锁脚本
    private static final String UNLOCK_FOR_READ_SCRIPT = SyncUtils.getScript("META-INFO/server/redis/rw-lock/ReentrantReadLock-unlock.lua");
    // 加写锁脚本
    private static final String LOCK_FOR_WRITE_SCRIPT = SyncUtils.getScript("META-INFO/server/redis/rw-lock/ReentrantWriteLock-lock.lua");
    // 解写锁脚本
    private static final String UNLOCK_FOR_WRITE_SCRIPT = SyncUtils.getScript("META-INFO/server/redis/rw-lock/ReentrantWriteLock-unlock.lua");
    // 同步通道前缀
    private static final String SYNC_CHANNEL_PREFIX = "sync:";
    // redis中key的前缀
    private static final String REDIS_KEY_PREFIX = "rw-lock:";

    // 读锁维护器
    private final SyncMaintainer readLockMaintainer = new SyncMaintainer();
    // 写锁维护器
    private final SyncMaintainer writeLockMaintainer = new SyncMaintainer();
    // redis执行器
    private final RedisExecutor redisExecutor;
    // 存活时间（毫秒）
    private final long liveTime;
    // 维护执行器
    private final Executor maintainExecutor;

    /**
     * 加读锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     * @return null 加锁成功；否则返回需等待的时间（毫秒）
     */
    public Long lockForRead(String key, String lockerId) {
        String redisKey = computeRedisKey(key);
        Long waitTime = redisExecutor.eval(
                LOCK_FOR_READ_SCRIPT,
                Collections.singletonList(redisKey),
                Arrays.asList(lockerId, liveTime, System.currentTimeMillis()),
                Long.class);
        if (waitTime == null) {
            readLockMaintainer.add(key, lockerId);
        }
        return waitTime;
    }

    /**
     * 解读锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     */
    public void unlockForRead(String key, String lockerId) {
        readLockMaintainer.remove(key, lockerId);
        String redisKey = computeRedisKey(key);
        boolean success = redisExecutor.eval(
                UNLOCK_FOR_READ_SCRIPT,
                Collections.singletonList(redisKey),
                Arrays.asList(lockerId, computeSyncChannel(key), System.currentTimeMillis()),
                Boolean.class);
        if (!success) {
            log.warn("调用redis解读锁异常，可能已经发生并发问题：key={},lockerId={}", key, lockerId);
        }
    }

    /**
     * 加写锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     * @param deadline 截止时间
     * @return null 加锁成功；否则返回需等待的时间（毫秒）
     */
    public Long lockForWrite(String key, String lockerId, long deadline) {
        String redisKey = computeRedisKey(key);
        Long waitTime = redisExecutor.eval(
                LOCK_FOR_WRITE_SCRIPT,
                Collections.singletonList(redisKey),
                Arrays.asList(lockerId, liveTime, deadline),
                Long.class);
        if (waitTime == null) {
            writeLockMaintainer.add(key, lockerId);
        }
        return waitTime;
    }

    /**
     * 解写锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     */
    public void unlockForWrite(String key, String lockerId) {
        writeLockMaintainer.remove(key, lockerId);
        String redisKey = computeRedisKey(key);
        boolean success = redisExecutor.eval(
                UNLOCK_FOR_WRITE_SCRIPT,
                Collections.singletonList(redisKey),
                Arrays.asList(lockerId, computeSyncChannel(key)),
                Boolean.class);
        if (!success) {
            log.warn("调用redis解写锁异常，可能已经发生并发问题：key={},lockerId={}", key, lockerId);
        }
    }

    /**
     * 维护
     */
    public void maintain() {
        Set<String> keys = readLockMaintainer.getKeys();
        for (String key : keys) {
            maintainExecutor.execute(() -> readLockMaintainer.maintain(key, (k, lockerId) -> redisExecutor.expire(k, liveTime, TimeUnit.MILLISECONDS)));
        }
        keys = writeLockMaintainer.getKeys();
        for (String key : keys) {
            maintainExecutor.execute(() -> writeLockMaintainer.maintain(key, (k, lockerId) -> redisExecutor.expire(k, liveTime, TimeUnit.MILLISECONDS)));
        }
    }

    /**
     * 计算同步通道
     *
     * @param key 锁标识
     * @return 同步通道
     */
    public String computeSyncChannel(String key) {
        return SYNC_CHANNEL_PREFIX + computeRedisKey(key);
    }

    // 计算在redis中key
    private String computeRedisKey(String key) {
        return REDIS_KEY_PREFIX + key;
    }
}
