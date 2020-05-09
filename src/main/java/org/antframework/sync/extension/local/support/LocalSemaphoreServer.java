/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-09 19:41 创建
 */
package org.antframework.sync.extension.local.support;

import org.antframework.sync.common.FiniteResource;

import java.util.concurrent.TimeUnit;

/**
 * 本地信号量服务端
 */
public class LocalSemaphoreServer {
    // 有限资源
    private final FiniteResource finiteResource = new FiniteResource();
    // 监听器管理器
    private final ListenerManager listenerManager = new ListenerManager();

    /**
     * 获取信号量许可
     *
     * @param key          信号量标识
     * @param totalPermits 许可总数
     * @param newPermits   新许可数
     * @param semaphorerId 获取信号量许可者id
     * @return null 获取成功；否则返回需等待的时间（毫秒）
     */
    public Long acquire(String key, int totalPermits, int newPermits, String semaphorerId) {
        Long waitTime = null;
        boolean success = finiteResource.acquire(key, semaphorerId, newPermits, totalPermits);
        if (!success) {
            waitTime = TimeUnit.HOURS.toMillis(1);
        }
        return waitTime;
    }

    /**
     * 释放信号量许可
     *
     * @param key          信号量标识
     * @param newPermits   新许可数
     * @param semaphorerId 获取信号量许可者id
     */
    public void release(String key, int newPermits, String semaphorerId) {
        finiteResource.release(key, semaphorerId, newPermits);
        listenerManager.publish(key);
    }

    /**
     * 新增同步监听器
     *
     * @param key      锁标识
     * @param listener 监听器
     */
    public void addSyncListener(String key, Runnable listener) {
        listenerManager.add(key, listener);
    }

    /**
     * 删除同步监听器
     *
     * @param key      锁标识
     * @param listener 监听器
     */
    public void removeSyncListener(String key, Runnable listener) {
        listenerManager.remove(key, listener);
    }
}
