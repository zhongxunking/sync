/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-07 21:08 创建
 */
package org.antframework.sync.extension.redis.support;

import lombok.extern.slf4j.Slf4j;
import org.antframework.sync.common.SyncUtils;
import org.antframework.sync.extension.redis.extension.RedisExecutor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * 基于redis的读写锁服务端
 */
@Slf4j
public class RedisRWLockServer {
    // 源加读锁脚本
    private static final String LOCK_FOR_READ_SCRIPT_SOURCE = SyncUtils.getScript("META-INF/server/redis/rw-lock/RWLock-lockForRead.lua");
    // 源解读锁脚本
    private static final String UNLOCK_FOR_READ_SCRIPT_SOURCE = SyncUtils.getScript("META-INF/server/redis/rw-lock/RWLock-unlockForRead.lua");
    // 源维护读锁脚本
    private static final String MAINTAIN_FOR_READ_SCRIPT_SOURCE = SyncUtils.getScript("META-INF/server/redis/rw-lock/RWLock-maintainForRead.lua");
    // 源加写锁脚本
    private static final String LOCK_FOR_WRITE_SCRIPT_SOURCE = SyncUtils.getScript("META-INF/server/redis/rw-lock/RWLock-lockForWrite.lua");
    // 源解写锁脚本
    private static final String UNLOCK_FOR_WRITE_SCRIPT_SOURCE = SyncUtils.getScript("META-INF/server/redis/rw-lock/RWLock-unlockForWrite.lua");
    // 源维护写锁脚本
    private static final String MAINTAIN_FOR_WRITE_SCRIPT_SOURCE = SyncUtils.getScript("META-INF/server/redis/rw-lock/RWLock-maintainForWrite.lua");
    // redis中key的前缀
    private static final String REDIS_KEY_PREFIX = "sync:rw-lock:";

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

    // 加读锁脚本
    private final Object lockForReadScript;
    // 解读锁脚本
    private final Object unlockForReadScript;
    // 维护读锁脚本
    private final Object maintainForReadScript;
    // 加写锁脚本
    private final Object lockForWriteScript;
    // 解写锁脚本
    private final Object unlockForWriteScript;
    // 维护写锁脚本
    private final Object maintainForWriteScript;

    public RedisRWLockServer(RedisExecutor redisExecutor, long liveTime, Executor maintainExecutor) {
        this.redisExecutor = redisExecutor;
        this.liveTime = liveTime;
        this.maintainExecutor = maintainExecutor;
        lockForReadScript = redisExecutor.encodeScript(LOCK_FOR_READ_SCRIPT_SOURCE, Long.class);
        unlockForReadScript = redisExecutor.encodeScript(UNLOCK_FOR_READ_SCRIPT_SOURCE, Boolean.class);
        maintainForReadScript = redisExecutor.encodeScript(MAINTAIN_FOR_READ_SCRIPT_SOURCE, Boolean.class);
        lockForWriteScript = redisExecutor.encodeScript(LOCK_FOR_WRITE_SCRIPT_SOURCE, Long.class);
        unlockForWriteScript = redisExecutor.encodeScript(UNLOCK_FOR_WRITE_SCRIPT_SOURCE, Boolean.class);
        maintainForWriteScript = redisExecutor.encodeScript(MAINTAIN_FOR_WRITE_SCRIPT_SOURCE, Boolean.class);
    }

    /**
     * 加读锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     * @return null 加锁成功；否则返回需等待的时间（毫秒）
     */
    public Long lockForRead(String key, String lockerId) {
        String redisKey = computeRedisKey(key);
        long currentTime = System.currentTimeMillis();
        Long waitTime = redisExecutor.eval(
                lockForReadScript,
                Collections.singletonList(redisKey),
                Arrays.asList(lockerId, currentTime, liveTime));
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
        long currentTime = System.currentTimeMillis();
        String syncChannel = computeSyncChannel(key);
        try {
            boolean success = redisExecutor.eval(
                    unlockForReadScript,
                    Collections.singletonList(redisKey),
                    Arrays.asList(lockerId, currentTime, syncChannel));
            if (!success) {
                log.error("调用redis解读锁失败（锁不存在或已经易主），可能已经发生并发问题：key={},lockerId={}", key, lockerId);
            }
        } catch (Throwable e) {
            log.error("调用redis解读锁出错：", e);
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
        long currentTime = System.currentTimeMillis();
        Long waitTime = redisExecutor.eval(
                lockForWriteScript,
                Collections.singletonList(redisKey),
                Arrays.asList(lockerId, deadline, currentTime, liveTime));
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
        String syncChannel = computeSyncChannel(key);
        try {
            boolean success = redisExecutor.eval(
                    unlockForWriteScript,
                    Collections.singletonList(redisKey),
                    Arrays.asList(lockerId, syncChannel));
            if (!success) {
                log.error("调用redis解写锁失败（锁不存在或已经易主），可能已经发生并发问题：key={},lockerId={}", key, lockerId);
            }
        } catch (Throwable e) {
            log.error("调用redis解写锁出错：", e);
        }
    }

    /**
     * 维护
     */
    public void maintain() {
        Set<String> keys = readLockMaintainer.getKeys();
        for (String key : keys) {
            maintainExecutor.execute(() -> readLockMaintainer.maintain(key, (k, lockerId) -> doMaintain(k, lockerId, maintainForReadScript, Arrays.asList(lockerId, System.currentTimeMillis(), liveTime))));
        }
        keys = writeLockMaintainer.getKeys();
        for (String key : keys) {
            maintainExecutor.execute(() -> writeLockMaintainer.maintain(key, (k, lockerId) -> doMaintain(k, lockerId, maintainForWriteScript, Arrays.asList(lockerId, liveTime))));
        }
    }

    // 执行维护
    private boolean doMaintain(String key, String lockerId, Object script, List<Object> args) {
        String redisKey = computeRedisKey(key);
        boolean alive = true;
        try {
            alive = redisExecutor.eval(
                    script,
                    Collections.singletonList(redisKey),
                    args);
            if (alive) {
                log.debug("调用redis维护读写锁成功：key={},lockerId={}", key, lockerId);
            } else {
                log.error("调用redis维护读写锁失败（锁不存在或已经易主），可能已经发生并发问题：key={},lockerId={}", key, lockerId);
            }
        } catch (Throwable e) {
            log.error("调用redis维护读写锁出错：", e);
        }
        return alive;
    }

    /**
     * 计算同步通道
     *
     * @param key 锁标识
     * @return 同步通道
     */
    public String computeSyncChannel(String key) {
        return computeRedisKey(key);
    }

    // 计算在redis中key
    private String computeRedisKey(String key) {
        return REDIS_KEY_PREFIX + key;
    }
}
