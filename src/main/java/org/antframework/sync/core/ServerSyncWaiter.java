/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-04-30 12:31 创建
 */
package org.antframework.sync.core;

import lombok.AllArgsConstructor;
import org.antframework.sync.extension.Server;

import java.util.concurrent.TimeUnit;

/**
 *
 */
@AllArgsConstructor
public class ServerSyncWaiter implements SyncWaiter {

    private final Server server;

    private final Server.SyncType syncType;

    private final String key;

    private final long maxWaitTime;

    @Override
    public boolean waitSync(long timeout, TimeUnit unit) throws InterruptedException {
        long time = Math.min(unit.toMillis(timeout), maxWaitTime);
        return server.waitSync(syncType, key, time, TimeUnit.MILLISECONDS);
    }

    @Override
    public String toString() {
        return String.format("ServerSyncWaiter{server=%s,syncType=%s,key=%s,maxWaitTime=%d}", server, syncType, key, maxWaitTime);
    }
}
