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
     * @param type    类型
     * @param waiter  等待者
     * @param timeout 超时时间（毫秒）
     * @return true 等到通知; false 等待超时
     * @throws InterruptedException
     */
    public boolean waitSync(String key, String type, String waiter, long timeout) throws InterruptedException {
        SyncListener syncListener = syncListeners.compute(key, (k, v) -> {
            if (v == null) {
                v = new SyncListener();
                server.addSyncListener(syncType, k, v);
            }
            v.addWaiter(type, waiter);
            return v;
        });
        return syncListener.waitSync(type, timeout);
    }

    /**
     * 删除等待者
     *
     * @param key    目标标识
     * @param type   类型
     * @param waiter 等待者
     */
    public void removeWaiter(String key, String type, String waiter) {
        syncListeners.computeIfPresent(key, (k, v) -> {
            v.removeWaiter(type, waiter);
            if (v.isEmpty()) {
                server.removeSyncListener(syncType, k, v);
                v = null;
            }
            return v;
        });
    }

    // 同步监听器
    private static class SyncListener implements Runnable {
        // 所有等待点
        private final Map<String, WaitPoint> waitPoints = new ConcurrentHashMap<>();

        @Override
        public synchronized void run() {
            // 唤醒所有等待者
            waitPoints.forEach((k, v) -> v.awakeWaiters());
        }

        // 等待同步
        boolean waitSync(String type, long timeout) throws InterruptedException {
            return waitPoints.get(type).waitSync(timeout);
        }

        // 添加等待者
        void addWaiter(String type, String waiter) {
            waitPoints.compute(type, (k, v) -> {
                if (v == null) {
                    v = new WaitPoint();
                }
                v.addWaiter(waiter);
                return v;
            });
        }

        // 删除等待者
        void removeWaiter(String type, String waiter) {
            waitPoints.computeIfPresent(type, (k, v) -> {
                v.removeWaiter(waiter);
                if (v.isEmpty()) {
                    v = null;
                }
                return v;
            });
        }

        // 是否为空
        boolean isEmpty() {
            return waitPoints.isEmpty();
        }

        // 等待点
        private static class WaitPoint {
            // 信号量（初始化为1的原因：缓解因监听消息前同步消息已经发出所导致的无意义的等待）
            private final Semaphore semaphore = new Semaphore(1);
            // 所有等待者
            private final Set<String> waiters = new HashSet<>();

            // 唤醒所有等待者
            void awakeWaiters() {
                // 唤醒所有等待者
                int permits = waiters.size() - semaphore.availablePermits();
                if (permits > 0) {
                    semaphore.release(permits);
                }
            }

            // 等待同步
            boolean waitSync(long timeout) throws InterruptedException {
                return semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
            }

            // 添加等待者
            void addWaiter(String waiter) {
                waiters.add(waiter);
            }

            // 删除等待者
            void removeWaiter(String waiter) {
                waiters.remove(waiter);
            }

            // 是否为空
            boolean isEmpty() {
                return waiters.isEmpty();
            }
        }
    }
}
