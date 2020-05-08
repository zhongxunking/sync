/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-07 21:23 创建
 */
package org.antframework.sync.extension.redis.support;

import lombok.AllArgsConstructor;
import org.antframework.sync.common.SyncUtils;
import org.antframework.sync.extension.redis.extension.RedisExecutor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * 基于redis的信号量服务端
 */
@AllArgsConstructor
public class RedisSemaphoreServer {
    // 更新许可数脚本
    private static final String UPDATE_PERMITS_SCRIPT = SyncUtils.getScript("META-INFO/server/redis/semaphore/Semaphore-updatePermits.lua");
    // 维护脚本
    private static final String MAINTAIN_SCRIPT = SyncUtils.getScript("META-INFO/server/redis/semaphore/Semaphore-maintain.lua");
    // 同步通道前缀
    private static final String SYNC_CHANNEL_PREFIX = "sync:";
    // redis中key的前缀
    private static final String REDIS_KEY_PREFIX = "semaphore:";

    // 维护器
    private final SyncMaintainer maintainer = new SyncMaintainer();
    // redis执行器
    private final RedisExecutor redisExecutor;
    // 存活时间（毫秒）
    private final long liveTime;
    // 维护执行器
    private final Executor maintainExecutor;

    /**
     * 获取许可
     *
     * @param key          信号量标识
     * @param totalPermits 许可总数
     * @param newPermits   新许可数
     * @param semaphorerId 获取信号量许可者id
     * @return null 获取成功；否则返回需等待的时间（毫秒）
     */
    public Long acquire(String key, int totalPermits, int newPermits, String semaphorerId) {
        String redisKey = computeRedisKey(key);
        Long waitTime = redisExecutor.eval(
                UPDATE_PERMITS_SCRIPT,
                Collections.singletonList(redisKey),
                Arrays.asList(
                        semaphorerId,
                        totalPermits,
                        newPermits,
                        false,
                        System.currentTimeMillis(),
                        liveTime,
                        computeSyncChannel(redisKey)),
                Long.class);
        if (waitTime == null && newPermits > 0) {
            maintainer.add(key, semaphorerId);
        }
        return waitTime;
    }

    /**
     * 释放许可
     *
     * @param key          信号量标识
     * @param totalPermits 许可总数
     * @param newPermits   新许可数
     * @param semaphorerId 获取信号量许可者id
     */
    public void release(String key, int totalPermits, int newPermits, String semaphorerId) {
        if (newPermits <= 0) {
            maintainer.remove(key, semaphorerId);
        }
        String redisKey = computeRedisKey(key);
        redisExecutor.eval(
                UPDATE_PERMITS_SCRIPT,
                Collections.singletonList(redisKey),
                Arrays.asList(
                        semaphorerId,
                        totalPermits,
                        newPermits,
                        true,
                        System.currentTimeMillis(),
                        liveTime,
                        computeSyncChannel(key)),
                Long.class);
    }

    /**
     * 维护
     */
    public void maintain() {
        Set<String> keys = maintainer.getKeys();
        for (String key : keys) {
            maintainExecutor.execute(() -> maintainer.maintain(key, (k, semaphorerId) -> redisExecutor.eval(
                    MAINTAIN_SCRIPT,
                    Collections.singletonList(k),
                    Arrays.asList(
                            semaphorerId,
                            System.currentTimeMillis(),
                            liveTime),
                    Boolean.class)));
        }
    }

    /**
     * 计算同步通道
     *
     * @param key 信号量标识
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
