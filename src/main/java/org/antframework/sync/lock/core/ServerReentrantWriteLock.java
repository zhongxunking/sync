/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-03 12:25 创建
 */
package org.antframework.sync.lock.core;

import org.antframework.sync.core.SyncExecutor;
import org.antframework.sync.core.SyncWaiter;
import org.antframework.sync.lock.support.RWLockServer;

/**
 * 基于服务端的可重入写锁
 */
public class ServerReentrantWriteLock extends AbstractServerReentrantLock {
    // 服务端
    private final RWLockServer server;

    public ServerReentrantWriteLock(String key, String lockerId, SyncExecutor syncExecutor, RWLockServer server) {
        super(key, lockerId, syncExecutor);
        this.server = server;
    }

    @Override
    protected void onTryLockEnd() {
        server.removeWaiter(getKey(), getLockerId());
    }

    @Override
    protected SyncWaiter lockInServer(long deadline) {
        return server.lockForWrite(getKey(), getLockerId(), deadline);
    }

    @Override
    protected void unlockInServer() {
        server.unlockForWrite(getKey(), getLockerId());
    }
}
