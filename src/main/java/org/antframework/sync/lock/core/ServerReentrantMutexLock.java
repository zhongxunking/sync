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
 *
 */
public class ServerReentrantMutexLock extends AbstractServerReentrantLock {

    private final MutexLockServer server;

    public ServerReentrantMutexLock(String lockerId, String key, SyncExecutor syncExecutor, MutexLockServer server) {
        super(lockerId, key, syncExecutor);
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
