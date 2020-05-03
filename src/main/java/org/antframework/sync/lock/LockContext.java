/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-04-29 12:49 创建
 */
package org.antframework.sync.lock;

import org.antframework.sync.common.SyncExecutor;
import org.antframework.sync.common.SyncUtils;
import org.antframework.sync.extension.Server;
import org.antframework.sync.lock.core.ServerReentrantMutexLock;
import org.antframework.sync.lock.core.ServerReentrantRWLock;
import org.antframework.sync.lock.support.MutexLockServer;
import org.antframework.sync.lock.support.RWLockServer;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * 锁上下文
 */
public class LockContext {
    // 锁持有器
    private final ThreadLocal<Map<String, Lock>> mutexLocks = ThreadLocal.withInitial(WeakHashMap::new);
    // 读写锁持有器
    private final ThreadLocal<Map<String, ReadWriteLock>> rwLocks = ThreadLocal.withInitial(WeakHashMap::new);
    // Sync执行器
    private final SyncExecutor syncExecutor;
    // 互斥锁服务端
    private final MutexLockServer mutexLockServer;
    // 读写锁服务端
    private final RWLockServer rwLockServer;

    public LockContext(SyncExecutor syncExecutor, Server server, long maxWaitTime) {
        this.syncExecutor = syncExecutor;
        this.mutexLockServer = new MutexLockServer(server, maxWaitTime);
        this.rwLockServer = new RWLockServer(server, maxWaitTime);
    }

    /**
     * 获取可重入互斥锁
     *
     * @param key 锁标识
     * @return 可重入互斥锁
     */
    public Lock getMutexLock(String key) {
        return mutexLocks.get().computeIfAbsent(key, k -> new ServerReentrantMutexLock(k, SyncUtils.newId(), syncExecutor, mutexLockServer));
    }

    /**
     * 获取可重入读写锁
     *
     * @param key 锁标识
     * @return 可重入读写锁
     */
    public ReadWriteLock getRWLock(String key) {
        return rwLocks.get().computeIfAbsent(key, k -> new ServerReentrantRWLock(k, SyncUtils.newId(), syncExecutor, rwLockServer));
    }
}
