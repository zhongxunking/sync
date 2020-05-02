/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-04-30 17:17 创建
 */
package org.antframework.sync.lock.support;

import lombok.AllArgsConstructor;
import org.antframework.sync.core.ServerSyncWaiter;
import org.antframework.sync.core.SyncWaiter;
import org.antframework.sync.extension.Server;

import java.util.Objects;

/**
 *
 */
@AllArgsConstructor
public class ReadWriteLockServer {

    private final MutextResource mutextResource = new MutextResource();
    private final Server server;

    private final long maxWaitTime;


    public SyncWaiter lockForRead(String key, String lockerId, long deadline) {
        Long waitTime = maxWaitTime;
        String exitingLockerId = mutextResource.peek(key);
        boolean localSuccess = exitingLockerId == null || Objects.equals(exitingLockerId, lockerId);
        if (localSuccess) {
            waitTime = server.lockForRead(key, lockerId, deadline);
        }
        SyncWaiter waiter = null;
        if (waitTime != null) {
            waitTime = Math.min(waitTime, maxWaitTime);
            waiter = new ServerSyncWaiter(server, Server.SyncType.READ_LOCK, key, waitTime);
        }
        return waiter;
    }

    public void unlockForRead(String key, String lockerId) {
        server.unlockForRead(key, lockerId);
    }

    public SyncWaiter lockForWrite(String key, String lockerId, long deadline) {
        Long waitTime = maxWaitTime;
        boolean localSuccess = mutextResource.acquire(key, lockerId);
        if (localSuccess) {
            try {
                waitTime = server.lockForWrite(key, lockerId, deadline);
            } finally {
                if (waitTime != null) {
                    mutextResource.release(key, lockerId);
                }
            }
        }
        SyncWaiter waiter = null;
        if (waitTime != null) {
            waitTime = Math.min(waitTime, maxWaitTime);
            waiter = new ServerSyncWaiter(server, Server.SyncType.WRITE_LOCK, key, waitTime);
        }
        return waiter;
    }

    public void unlockForWrite(String key, String lockerId) {
        mutextResource.release(key, lockerId);
        server.unlockForWrite(key, lockerId);
    }
}
