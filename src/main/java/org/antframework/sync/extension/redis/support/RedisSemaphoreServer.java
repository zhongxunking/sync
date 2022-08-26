/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-07 21:23 创建
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
 * 基于redis的信号量服务端
 */
@Slf4j
public class RedisSemaphoreServer {
    // 源更新许可数脚本
    private static final String UPDATE_PERMITS_SCRIPT_SOURCE = SyncUtils.getScript("META-INF/server/redis/semaphore/Semaphore-updatePermits.lua");
    // 源维护脚本
    private static final String MAINTAIN_SCRIPT_SOURCE = SyncUtils.getScript("META-INF/server/redis/semaphore/Semaphore-maintain.lua");

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

    // 更新许可数脚本
    private final Object updatePermitsScript;
    // 维护脚本
    private final Object maintainScript;

    public RedisSemaphoreServer(BiFunction<Server.SyncType, String, String> keyGenerator,
                                RedisExecutor redisExecutor,
                                long liveTime,
                                Executor maintainExecutor) {
        this.keyGenerator = keyGenerator;
        this.redisExecutor = redisExecutor;
        this.liveTime = liveTime;
        this.maintainExecutor = maintainExecutor;
        updatePermitsScript = redisExecutor.encodeScript(UPDATE_PERMITS_SCRIPT_SOURCE, Long.class);
        maintainScript = redisExecutor.encodeScript(MAINTAIN_SCRIPT_SOURCE, Boolean.class);
    }

    /**
     * 获取许可
     *
     * @param key          信号量标识
     * @param semaphorerId 获取信号量许可者id
     * @param newPermits   新许可数
     * @param totalPermits 许可总数
     * @return null 获取成功；否则返回需等待的时间（毫秒）
     */
    public Long acquire(String key, String semaphorerId, int newPermits, int totalPermits) {
        String redisKey = computeRedisKey(key);
        long currentTime = System.currentTimeMillis();
        String syncChannel = computeSyncChannel(key);
        Long waitTime = redisExecutor.eval(
                updatePermitsScript,
                Collections.singletonList(redisKey),
                Arrays.asList(
                        semaphorerId,
                        newPermits,
                        totalPermits,
                        currentTime,
                        syncChannel,
                        liveTime,
                        false));
        if (waitTime == null && newPermits > 0) {
            maintainer.add(key, semaphorerId);
        }
        return waitTime;
    }

    /**
     * 释放许可
     *
     * @param key          信号量标识
     * @param semaphorerId 获取信号量许可者id
     * @param newPermits   新许可数
     * @param totalPermits 许可总数
     */
    public void release(String key, String semaphorerId, int newPermits, int totalPermits) {
        if (newPermits <= 0) {
            maintainer.remove(key, semaphorerId);
        }
        String redisKey = computeRedisKey(key);
        long currentTime = System.currentTimeMillis();
        String syncChannel = computeSyncChannel(key);
        try {
            redisExecutor.eval(
                    updatePermitsScript,
                    Collections.singletonList(redisKey),
                    Arrays.asList(
                            semaphorerId,
                            newPermits,
                            totalPermits,
                            currentTime,
                            syncChannel,
                            liveTime,
                            true));
        } catch (Throwable e) {
            log.error("调用redis释放信号量许可出错：", e);
        }
    }

    /**
     * 维护
     */
    public void maintain() {
        Set<String> keys = maintainer.getKeys();
        for (String key : keys) {
            maintainExecutor.execute(() -> maintainer.maintain(key, (k, semaphorerId) -> {
                String redisKey = computeRedisKey(k);
                long currentTime = System.currentTimeMillis();
                boolean alive = true;
                try {
                    alive = redisExecutor.eval(
                            maintainScript,
                            Collections.singletonList(redisKey),
                            Arrays.asList(
                                    semaphorerId,
                                    currentTime,
                                    liveTime));
                    if (alive) {
                        log.debug("调用redis维护信号量成功：key={},semaphorerId={}", k, semaphorerId);
                    } else {
                        log.error("调用redis维护信号量失败（信号量不存在或已经不持有许可），可能已经发生并发问题：key={},semaphorerId={}", k, semaphorerId);
                    }
                } catch (Throwable e) {
                    log.error("调用redis维护信号量出错：", e);
                }
                return alive;
            }));
        }
    }

    /**
     * 计算同步通道
     *
     * @param key 信号量标识
     * @return 同步通道
     */
    public String computeSyncChannel(String key) {
        return computeRedisKey(key);
    }

    // 计算在redis中key
    private String computeRedisKey(String key) {
        return keyGenerator.apply(Server.SyncType.SEMAPHORE, key);
    }
}
