/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-04-30 12:31 创建
 */
package org.antframework.sync.common;

import lombok.AllArgsConstructor;

/**
 * 基于服务端的同步等待器
 */
@AllArgsConstructor
public class ServerSyncWaiter implements SyncWaiter {
    // 同步管理者
    private final ServerSyncManager syncManager;
    // 目标标识
    private final String key;
    // 类型
    private final String type;
    // 等待者
    private final String waiter;
    // 最大等待时长（毫秒）
    private final long maxWaitTime;

    @Override
    public boolean waitSync(long timeout) throws InterruptedException {
        long time = Math.min(timeout, maxWaitTime);
        return syncManager.waitSync(key, type, waiter, time);
    }

    @Override
    public String toString() {
        return String.format("ServerSyncWaiter{key=%s,type=%s,waiter=%s,maxWaitTime=%d}", key, type, waiter, maxWaitTime);
    }
}
