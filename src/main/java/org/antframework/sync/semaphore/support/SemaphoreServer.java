/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-05 13:56 创建
 */
package org.antframework.sync.semaphore.support;

import org.antframework.sync.common.FiniteResource;
import org.antframework.sync.common.ServerSyncManager;
import org.antframework.sync.common.ServerSyncWaiter;
import org.antframework.sync.common.SyncWaiter;
import org.antframework.sync.extension.Server;

/**
 * 信号量服务端
 */
public class SemaphoreServer {
    // 有限资源
    private final FiniteResource finiteResource = new FiniteResource();
    // 服务端
    private final Server server;
    // 最大等待时间
    private final long maxWaitTime;
    // 同步管理者
    private final ServerSyncManager syncManager;

    public SemaphoreServer(Server server, long maxWaitTime) {
        this.server = server;
        this.maxWaitTime = maxWaitTime;
        this.syncManager = new ServerSyncManager(Server.SyncType.SEMAPHORE, server);
    }

    /**
     * 获取许可
     *
     * @param key          信号量标识
     * @param totalPermits 许可总数
     * @param newPermits   新许可数
     * @param semaphorerId 获取信号量许可者id
     * @param deadline     截止时间
     * @return null 获取成功；否则返回需等待的时间
     */
    public SyncWaiter acquire(String key, int totalPermits, int newPermits, String semaphorerId, long deadline) {
        Long waitTime = maxWaitTime;
        int oldPermits = finiteResource.peek(key, semaphorerId);
        boolean localSuccess = finiteResource.acquire(key, semaphorerId, newPermits, totalPermits);
        if (localSuccess) {
            try {
                waitTime = server.acquireForSemaphore(key, totalPermits, newPermits, semaphorerId, deadline);
            } finally {
                if (waitTime != null) {
                    finiteResource.release(key, semaphorerId, oldPermits);
                }
            }
        }
        SyncWaiter waiter = null;
        if (waitTime != null) {
            waitTime = Math.min(waitTime, maxWaitTime);
            waiter = new ServerSyncWaiter(syncManager, key, semaphorerId, waitTime);
        }
        return waiter;
    }

    /**
     * 释放许可
     *
     * @param key          信号量标识
     * @param totalPermits 许可总数
     * @param newPermits   新许可数
     * @param semaphorerId 获取信号量许可者id
     */
    public void release(String key, int totalPermits, int newPermits, String semaphorerId) {
        finiteResource.release(key, semaphorerId, newPermits);
        server.releaseForSemaphore(key, totalPermits, newPermits, semaphorerId);
    }

    /**
     * 删除等待者
     *
     * @param key          信号量标识
     * @param semaphorerId 获取信号量许可者id
     */
    public void removeWaiter(String key, String semaphorerId) {
        syncManager.removeWaiter(key, semaphorerId);
    }
}
