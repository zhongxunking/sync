/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-09 17:34 创建
 */
package org.antframework.sync.extension.local.support;

import org.antframework.sync.common.MutexResource;

import java.util.concurrent.TimeUnit;

/**
 * 本地互斥锁服务端
 */
public class LocalMutexLockServer {
    // 互斥资源
    private final MutexResource mutexResource = new MutexResource();
    // 监听器管理器
    private final ListenerManager listenerManager = new ListenerManager();

    /**
     * 加锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     * @return null 加锁成功；否则返回需等待的时间（毫秒）
     */
    public Long lock(String key, String lockerId) {
        Long waitTime = null;
        boolean success = mutexResource.acquire(key, lockerId);
        if (!success) {
            waitTime = TimeUnit.HOURS.toMillis(1);
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
        mutexResource.release(key, lockerId);
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
