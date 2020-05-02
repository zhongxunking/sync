/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-04-29 18:04 创建
 */
package org.antframework.sync.lock.support;

import lombok.AllArgsConstructor;
import org.antframework.sync.core.ServerSyncWaiter;
import org.antframework.sync.core.SyncWaiter;
import org.antframework.sync.extension.Server;

/**
 * 互斥锁服务端
 */
@AllArgsConstructor
public class MutexLockServer {
    // 互斥资源
    private final MutexResource mutexResource = new MutexResource();
    // 服务端
    private final Server server;
    // 最大等待时间
    private final long maxWaitTime;

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
            waiter = new ServerSyncWaiter(server, Server.SyncType.MUTEX_LOCK, key, waitTime);
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
     * 删除锁的等待器
     *
     * @param key 锁标识
     */
    public void removeWaiter(String key) {
        server.removeWaiter(Server.SyncType.MUTEX_LOCK, key);
    }
}
