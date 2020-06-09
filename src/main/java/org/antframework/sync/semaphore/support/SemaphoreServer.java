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
    // 等待者类型
    private static final String WAITER_TYPE = "semaphore";

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
     * @param semaphorerId 获取信号量许可者id
     * @param newPermits   新许可数
     * @param totalPermits 许可总数
     * @param deadline     截止时间
     * @return null 获取成功；否则返回需等待的时间
     */
    public SyncWaiter acquire(String key, String semaphorerId, int newPermits, int totalPermits, long deadline) {
        Long waitTime = maxWaitTime;
        int oldPermits = finiteResource.peek(key, semaphorerId);
        boolean localSuccess = finiteResource.acquire(key, semaphorerId, newPermits, totalPermits);
        if (localSuccess) {
            try {
                waitTime = server.acquireForSemaphore(key, semaphorerId, newPermits, totalPermits, deadline);
            } finally {
                if (waitTime != null) {
                    finiteResource.release(key, semaphorerId, oldPermits);
                }
            }
        }
        SyncWaiter waiter = null;
        if (waitTime != null) {
            waitTime = Math.min(waitTime, maxWaitTime);
            waiter = new ServerSyncWaiter(syncManager, key, WAITER_TYPE, semaphorerId, waitTime);
        }
        return waiter;
    }

    /**
     * 释放许可
     *
     * @param key          信号量标识
     * @param semaphorerId 获取信号量许可者id
     * @param newPermits   新许可数
     * @param totalPermits 许可总数
     */
    public void release(String key, String semaphorerId, int newPermits, int totalPermits) {
        finiteResource.release(key, semaphorerId, newPermits);
        server.releaseForSemaphore(key, semaphorerId, newPermits, totalPermits);
    }

    /**
     * 删除等待者
     *
     * @param key          信号量标识
     * @param semaphorerId 获取信号量许可者id
     */
    public void removeWaiter(String key, String semaphorerId) {
        syncManager.removeWaiter(key, WAITER_TYPE, semaphorerId);
    }
}
