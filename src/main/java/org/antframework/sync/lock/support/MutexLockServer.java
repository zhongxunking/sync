/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-04-29 18:04 创建
 */
package org.antframework.sync.lock.support;

import lombok.AllArgsConstructor;
import org.antframework.sync.core.ServerSyncWaiter;
import org.antframework.sync.core.SyncWaiter;
import org.antframework.sync.extension.Server;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
@AllArgsConstructor
public class MutexLockServer {

    private final Map<String, String> keyLockers = new ConcurrentHashMap<>();

    private final Server server;

    private final long maxWaitTime;

    public SyncWaiter lock(String key, String lockerId, long deadline) {
        AtomicBoolean localSuccess = new AtomicBoolean(false);
        keyLockers.compute(key, (k, v) -> {
            if (v == null || Objects.equals(v, lockerId)) {
                v = lockerId;
                localSuccess.set(true);
            }
            return v;
        });
        Long waitTime = maxWaitTime;
        if (localSuccess.get()) {
            try {
                waitTime = server.lockForMutex(key, lockerId, deadline);
            } finally {
                if (waitTime != null) {
                    keyLockers.remove(key);
                }
            }
        }
        if (waitTime != null) {
            waitTime = Math.min(waitTime, maxWaitTime);
            return new ServerSyncWaiter(server, Server.SyncType.MUTEX_LOCK, key, waitTime);
        }
        return null;
    }

    public void unlock(String key, String lockerId) {
        keyLockers.compute(key, (k, v) -> {
            if (Objects.equals(v, lockerId)) {
                v = null;
            }
            return v;
        });
        server.unlockForMutex(key, lockerId);
    }

    public void removeWaiter(String key) {
        server.removeWaiter(Server.SyncType.MUTEX_LOCK, key);
    }
}
