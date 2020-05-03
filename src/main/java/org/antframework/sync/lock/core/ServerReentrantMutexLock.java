/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-04-30 16:56 创建
 */
package org.antframework.sync.lock.core;

import org.antframework.sync.core.SyncExecutor;
import org.antframework.sync.core.SyncWaiter;
import org.antframework.sync.lock.support.MutexLockServer;

/**
 * 基于服务端的可重入互斥锁
 */
public class ServerReentrantMutexLock extends AbstractServerReentrantLock {
    // 互斥锁服务端
    private final MutexLockServer server;

    public ServerReentrantMutexLock(String key, String lockerId, SyncExecutor syncExecutor, MutexLockServer server) {
        super(key, lockerId, syncExecutor);
        this.server = server;
    }

    @Override
    protected void onTryLockEnd() {
        server.removeWaiter(getKey());
    }

    @Override
    protected SyncWaiter lockInServer(long deadline) {
        return server.lock(getKey(), getLockerId(), deadline);
    }

    @Override
    protected void unlockInServer() {
        server.unlock(getKey(), getLockerId());
    }
}