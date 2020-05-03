/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-04-30 12:31 创建
 */
package org.antframework.sync.core;

import lombok.AllArgsConstructor;

import java.util.concurrent.TimeUnit;

/**
 * 基于服务端的同步等待器
 */
@AllArgsConstructor
public class ServerSyncWaiter implements SyncWaiter {
    // 同步管理者
    private final ServerSyncManager syncManager;
    // 目标标识
    private final String key;
    // 等待者
    private final String waiter;
    // 最大等待时长（毫秒）
    private final long maxWaitTime;

    @Override
    public boolean waitSync(long timeout, TimeUnit unit) throws InterruptedException {
        long time = Math.min(unit.toMillis(timeout), maxWaitTime);
        return syncManager.waitSync(key, waiter, time);
    }

    @Override
    public String toString() {
        return String.format("ServerSyncWaiter{key=%s,waiter=%s,maxWaitTime=%d}", key, waiter, maxWaitTime);
    }
}
