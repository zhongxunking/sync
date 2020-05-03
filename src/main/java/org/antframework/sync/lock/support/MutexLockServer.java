/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-04-29 18:04 创建
 */
package org.antframework.sync.lock.support;

import org.antframework.sync.common.ServerSyncManager;
import org.antframework.sync.common.ServerSyncWaiter;
import org.antframework.sync.common.SyncWaiter;
import org.antframework.sync.extension.Server;

/**
 * 互斥锁服务端
 */
public class MutexLockServer {
    // 互斥资源
    private final MutexResource mutexResource = new MutexResource();
    // 服务端
    private final Server server;
    // 最大等待时间
    private final long maxWaitTime;
    // 同步管理者
    private final ServerSyncManager syncManager;

    public MutexLockServer(Server server, long maxWaitTime) {
        this.server = server;
        this.maxWaitTime = maxWaitTime;
        this.syncManager = new ServerSyncManager(Server.SyncType.MUTEX_LOCK, server);
    }

    /**
     * 加锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     * @param deadline 截止时间
     * @return null 加锁成功；否则失败
     */
    public SyncWaiter lock(String key, String lockerId, long deadline) {
        Long waitTime = maxWaitTime;
        boolean localSuccess = mutexResource.acquire(key, lockerId);
        if (localSuccess) {
            try {
                waitTime = server.lockForMutex(key, lockerId, deadline);
            } finally {
                if (waitTime != null) {
                    mutexResource.release(key, lockerId);
                }
            }
        }
        SyncWaiter waiter = null;
        if (waitTime != null) {
            waitTime = Math.min(waitTime, maxWaitTime);
            waiter = new ServerSyncWaiter(syncManager, key, lockerId, waitTime);
        }
        return waiter;
    }

    /**
     * 解锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     */
    public void unlock(String key, String lockerId) {
        mutexResource.release(key, lockerId);
        server.unlockForMutex(key, lockerId);
    }

    /**
     * 删除等待者
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     */
    public void removeWaiter(String key, String lockerId) {
        syncManager.removeWaiter(key, lockerId);
    }
}
