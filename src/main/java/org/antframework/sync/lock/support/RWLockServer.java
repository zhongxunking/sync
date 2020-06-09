/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-04-30 17:17 创建
 */
package org.antframework.sync.lock.support;

import lombok.AllArgsConstructor;
import org.antframework.sync.common.MutexResource;
import org.antframework.sync.common.ServerSyncManager;
import org.antframework.sync.common.ServerSyncWaiter;
import org.antframework.sync.common.SyncWaiter;
import org.antframework.sync.extension.Server;

import java.util.Objects;

/**
 * 读写锁服务端
 */
@AllArgsConstructor
public class RWLockServer {
    // 读等待者类型
    private static final String READER_WAITER_TYPE = "reader";
    // 写等待者类型
    private static final String WRITER_WAITER_TYPE = "writer";

    // 互斥资源
    private final MutexResource mutexResource = new MutexResource();
    // 服务端
    private final Server server;
    // 最大等待时间
    private final long maxWaitTime;
    // 同步管理者
    private final ServerSyncManager syncManager;

    public RWLockServer(Server server, long maxWaitTime) {
        this.server = server;
        this.maxWaitTime = maxWaitTime;
        this.syncManager = new ServerSyncManager(Server.SyncType.RW_LOCK, server);
    }

    /**
     * 加读锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     * @param deadline 截止时间
     * @return null 加锁成功；否则失败
     */
    public SyncWaiter lockForRead(String key, String lockerId, long deadline) {
        Long waitTime = maxWaitTime;
        String exitingLockerId = mutexResource.peek(key);
        boolean localSuccess = exitingLockerId == null || Objects.equals(exitingLockerId, lockerId);
        if (localSuccess) {
            waitTime = server.lockForRead(key, lockerId, deadline);
        }
        SyncWaiter waiter = null;
        if (waitTime != null) {
            waitTime = Math.min(waitTime, maxWaitTime);
            waiter = new ServerSyncWaiter(syncManager, key, READER_WAITER_TYPE, lockerId, waitTime);
        }
        return waiter;
    }

    /**
     * 解读锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     */
    public void unlockForRead(String key, String lockerId) {
        server.unlockForRead(key, lockerId);
    }

    /**
     * 加写锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     * @param deadline 截止时间
     * @return null 加锁成功；否则失败
     */
    public SyncWaiter lockForWrite(String key, String lockerId, long deadline) {
        Long waitTime = maxWaitTime;
        boolean localSuccess = mutexResource.acquire(key, lockerId);
        if (localSuccess) {
            try {
                waitTime = server.lockForWrite(key, lockerId, deadline);
            } finally {
                if (waitTime != null) {
                    mutexResource.release(key, lockerId);
                }
            }
        }
        SyncWaiter waiter = null;
        if (waitTime != null) {
            waitTime = Math.min(waitTime, maxWaitTime);
            waiter = new ServerSyncWaiter(syncManager, key, WRITER_WAITER_TYPE, lockerId, waitTime);
        }
        return waiter;
    }

    /**
     * 解写锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     */
    public void unlockForWrite(String key, String lockerId) {
        mutexResource.release(key, lockerId);
        server.unlockForWrite(key, lockerId);
    }

    /**
     * 删除读等待者
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     */
    public void removeReaderWaiter(String key, String lockerId) {
        syncManager.removeWaiter(key, READER_WAITER_TYPE, lockerId);
    }

    /**
     * 删除写等待者
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     */
    public void removeWriterWaiter(String key, String lockerId) {
        syncManager.removeWaiter(key, WRITER_WAITER_TYPE, lockerId);
    }
}
