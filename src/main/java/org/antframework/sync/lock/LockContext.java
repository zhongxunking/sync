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
import java.util.function.Function;

/**
 * 锁上下文
 */
public class LockContext {
    // 锁持有器
    private final ThreadLocal<Map<String, Lock>> mutexLocks = ThreadLocal.withInitial(WeakHashMap::new);
    // 读写锁持有器
    private final ThreadLocal<Map<String, ReadWriteLock>> rwLocks = ThreadLocal.withInitial(WeakHashMap::new);
    // key转换器
    private final Function<Object, String> keyConverter;
    // Sync执行器
    private final SyncExecutor syncExecutor;
    // 互斥锁服务端
    private final MutexLockServer mutexLockServer;
    // 读写锁服务端
    private final RWLockServer rwLockServer;

    public LockContext(Function<Object, String> keyConverter,
                       SyncExecutor syncExecutor,
                       Server server,
                       long maxWaitTime) {
        this.keyConverter = keyConverter;
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
    public Lock getLock(Object key) {
        return mutexLocks.get().computeIfAbsent(convertKey(key), k -> new ServerReentrantMutexLock(k, SyncUtils.newId(), syncExecutor, mutexLockServer));
    }

    /**
     * 获取可重入读写锁
     *
     * @param key 锁标识
     * @return 可重入读写锁
     */
    public ReadWriteLock getRWLock(Object key) {
        return rwLocks.get().computeIfAbsent(convertKey(key), k -> new ServerReentrantRWLock(k, SyncUtils.newId(), syncExecutor, rwLockServer));
    }

    // 转换key
    private String convertKey(Object key) {
        String convertedKey = keyConverter.apply(key);
        if (convertedKey == null) {
            throw new IllegalArgumentException(String.format("转换后的key不能为null(原始key:%s)", key));
        }
        return convertedKey;
    }
}
