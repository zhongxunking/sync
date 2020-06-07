/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-03 11:05 创建
 */
package org.antframework.sync.common;

import lombok.AllArgsConstructor;
import org.antframework.sync.extension.Server;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于服务端的同步管理者
 */
@AllArgsConstructor
public class ServerSyncManager {
    // 所有同步监听器
    private final Map<String, SyncListener> syncListeners = new ConcurrentHashMap<>();
    // 同步类型
    private final Server.SyncType syncType;
    // 服务端
    private final Server server;

    /**
     * 等待同步
     *
     * @param key     目标标识
     * @param waiter  等待者
     * @param timeout 超时时间（毫秒）
     * @return true 等到通知; false 等待超时
     * @throws InterruptedException
     */
    public boolean waitSync(String key, String waiter, long timeout) throws InterruptedException {
        AtomicBoolean newWaiter = new AtomicBoolean(false);
        SyncListener syncListener = syncListeners.compute(key, (k, v) -> {
            if (v == null) {
                v = new SyncListener();
                server.addSyncListener(syncType, k, v);
            }
            newWaiter.set(v.addWaiter(waiter));
            return v;
        });
        return !newWaiter.get() && syncListener.waitSync(timeout);
    }

    /**
     * 删除等待者
     *
     * @param key    目标标识
     * @param waiter 等待者
     */
    public void removeWaiter(String key, String waiter) {
        syncListeners.computeIfPresent(key, (k, v) -> {
            v.removeWaiter(waiter);
            if (v.getWaiterAmount() <= 0) {
                server.removeSyncListener(syncType, k, v);
                v = null;
            }
            return v;
        });
    }

    // 同步监听器
    private static class SyncListener implements Runnable {
        // 信号量
        private final Semaphore semaphore = new Semaphore(0, true);
        // 所有等待者
        private final Set<String> waiters = new HashSet<>();

        @Override
        public synchronized void run() {
            // 唤醒所有等待者
            int permits = getWaiterAmount() - semaphore.availablePermits();
            if (permits > 0) {
                semaphore.release(permits);
            }
        }

        // 等待同步
        boolean waitSync(long timeout) throws InterruptedException {
            return semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
        }

        // 添加等待者（返回true表示新的等待者；否则返回false）
        boolean addWaiter(String waiter) {
            return waiters.add(waiter);
        }

        // 删除等待者
        void removeWaiter(String waiter) {
            waiters.remove(waiter);
        }

        // 获取等待者数量
        int getWaiterAmount() {
            return waiters.size();
        }
    }
}
