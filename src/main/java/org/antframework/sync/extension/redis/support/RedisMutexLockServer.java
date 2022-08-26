/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-07 20:42 创建
 */
package org.antframework.sync.extension.redis.support;

import lombok.extern.slf4j.Slf4j;
import org.antframework.sync.common.SyncUtils;
import org.antframework.sync.extension.Server;
import org.antframework.sync.extension.redis.extension.RedisExecutor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

/**
 * 基于redis的互斥锁服务端
 */
@Slf4j
public class RedisMutexLockServer {
    // 源加锁脚本
    private static final String LOCK_SCRIPT_SOURCE = SyncUtils.getScript("META-INF/server/redis/mutex-lock/MutexLock-lock.lua");
    // 源解锁脚本
    private static final String UNLOCK_SCRIPT_SOURCE = SyncUtils.getScript("META-INF/server/redis/mutex-lock/MutexLock-unlock.lua");
    // 源维护脚本
    private static final String MAINTAIN_SCRIPT_SOURCE = SyncUtils.getScript("META-INF/server/redis/mutex-lock/MutexLock-maintain.lua");

    // 维护器
    private final SyncMaintainer maintainer = new SyncMaintainer();
    // key生成器
    private final BiFunction<Server.SyncType, String, String> keyGenerator;
    // redis执行器
    private final RedisExecutor redisExecutor;
    // 存活时间（毫秒）
    private final long liveTime;
    // 维护执行器
    private final Executor maintainExecutor;

    // 加锁脚本
    private final Object lockScript;
    // 解锁脚本
    private final Object unlockScript;
    // 维护脚本
    private final Object maintainScript;

    public RedisMutexLockServer(BiFunction<Server.SyncType, String, String> keyGenerator,
                                RedisExecutor redisExecutor,
                                long liveTime,
                                Executor maintainExecutor) {
        this.keyGenerator = keyGenerator;
        this.redisExecutor = redisExecutor;
        this.liveTime = liveTime;
        this.maintainExecutor = maintainExecutor;
        lockScript = redisExecutor.encodeScript(LOCK_SCRIPT_SOURCE, Long.class);
        unlockScript = redisExecutor.encodeScript(UNLOCK_SCRIPT_SOURCE, Boolean.class);
        maintainScript = redisExecutor.encodeScript(MAINTAIN_SCRIPT_SOURCE, Boolean.class);
    }

    /**
     * 加锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     * @return null 加锁成功；否则返回需等待的时间（毫秒）
     */
    public Long lock(String key, String lockerId) {
        String redisKey = computeRedisKey(key);
        Long waitTime = redisExecutor.eval(
                lockScript,
                Collections.singletonList(redisKey),
                Arrays.asList(lockerId, liveTime));
        if (waitTime == null) {
            maintainer.add(key, lockerId);
        }
        return waitTime;
    }

    /**
     * 解锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     */
    public void unlock(String key, String lockerId) {
        maintainer.remove(key, lockerId);
        String redisKey = computeRedisKey(key);
        String syncChannel = computeSyncChannel(key);
        try {
            boolean success = redisExecutor.eval(
                    unlockScript,
                    Collections.singletonList(redisKey),
                    Arrays.asList(lockerId, syncChannel));
            if (!success) {
                log.error("调用redis解互斥锁失败（锁不存在或已经易主），可能已经发生并发问题：key={},lockerId={}", key, lockerId);
            }
        } catch (Throwable e) {
            log.error("调用redis解互斥锁出错：", e);
        }
    }

    /**
     * 维护
     */
    public void maintain() {
        Set<String> keys = maintainer.getKeys();
        for (String key : keys) {
            maintainExecutor.execute(() -> maintainer.maintain(key, (k, lockerId) -> {
                String redisKey = computeRedisKey(k);
                boolean alive = true;
                try {
                    alive = redisExecutor.eval(
                            maintainScript,
                            Collections.singletonList(redisKey),
                            Arrays.asList(lockerId, liveTime));
                    if (alive) {
                        log.debug("调用redis维护互斥锁成功：key={},lockerId={}", k, lockerId);
                    } else {
                        log.error("调用redis维护互斥锁失败（锁不存在或已经易主），可能已经发生并发问题：key={},lockerId={}", k, lockerId);
                    }
                } catch (Throwable e) {
                    log.error("调用redis维护互斥锁出错：", e);
                }
                return alive;
            }));
        }
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
        return keyGenerator.apply(Server.SyncType.MUTEX_LOCK, key);
    }
}
