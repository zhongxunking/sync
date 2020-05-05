/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-05 16:24 创建
 */
package org.antframework.sync.semaphore.core;

import org.antframework.sync.common.SyncExecutor;
import org.antframework.sync.common.SyncWaiter;
import org.antframework.sync.semaphore.support.SemaphoreServer;

/**
 * 基于服务端的信号量的默认实现
 */
public class DefaultServerSemaphore extends AbstractServerSemaphore {
    // 服务端
    private final SemaphoreServer server;

    public DefaultServerSemaphore(String key, String semaphorerId, int totalPermits, SyncExecutor syncExecutor, SemaphoreServer server) {
        super(key, semaphorerId, totalPermits, syncExecutor);
        this.server = server;
    }

    @Override
    protected void onTryAcquireEnd() {
        server.removeWaiter(getKey(), getSemaphorerId());
    }

    @Override
    protected SyncWaiter acquireInServer(int newPermits, long deadline) {
        return server.acquire(getKey(), getTotalPermits(), newPermits, getSemaphorerId(), deadline);
    }

    @Override
    protected void releaseInServer(int newPermits) {
        server.release(getKey(), getTotalPermits(), newPermits, getSemaphorerId());
    }
}
