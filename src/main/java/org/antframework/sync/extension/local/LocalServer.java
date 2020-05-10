/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-09 17:27 创建
 */
package org.antframework.sync.extension.local;

import org.antframework.sync.extension.Server;
import org.antframework.sync.extension.local.support.LocalMutexLockServer;
import org.antframework.sync.extension.local.support.LocalRWLockServer;
import org.antframework.sync.extension.local.support.LocalSemaphoreServer;

/**
 * 本地服务端
 */
public class LocalServer implements Server {
    // 互斥锁服务端
    private final LocalMutexLockServer mutexLockServer = new LocalMutexLockServer();
    // 读写锁服务端
    private final LocalRWLockServer rwLockServer = new LocalRWLockServer();
    // 信号量服务端
    private final LocalSemaphoreServer semaphoreServer = new LocalSemaphoreServer();

    @Override
    public Long lockForMutex(String key, String lockerId, long deadline) {
        return mutexLockServer.lock(key, lockerId);
    }

    @Override
    public void unlockForMutex(String key, String lockerId) {
        mutexLockServer.unlock(key, lockerId);
    }

    @Override
    public Long lockForRead(String key, String lockerId, long deadline) {
        return rwLockServer.lockForRead(key, lockerId);
    }

    @Override
    public void unlockForRead(String key, String lockerId) {
        rwLockServer.unlockForRead(key, lockerId);
    }

    @Override
    public Long lockForWrite(String key, String lockerId, long deadline) {
        return rwLockServer.lockForWrite(key, lockerId, deadline);
    }

    @Override
    public void unlockForWrite(String key, String lockerId) {
        rwLockServer.unlockForWrite(key, lockerId);
    }

    @Override
    public Long acquireForSemaphore(String key, String semaphorerId, int newPermits, int totalPermits, long deadline) {
        return semaphoreServer.acquire(key, semaphorerId, newPermits, totalPermits);
    }

    @Override
    public void releaseForSemaphore(String key, String semaphorerId, int newPermits, int totalPermits) {
        semaphoreServer.release(key, semaphorerId, newPermits);
    }

    @Override
    public void addSyncListener(SyncType syncType, String key, Runnable listener) {
        switch (syncType) {
            case MUTEX_LOCK:
                mutexLockServer.addSyncListener(key, listener);
                break;
            case RW_LOCK:
                rwLockServer.addSyncListener(key, listener);
                break;
            case SEMAPHORE:
                semaphoreServer.addSyncListener(key, listener);
                break;
            default:
                throw new IllegalArgumentException("无法识别的Sync类型：" + syncType);
        }
    }

    @Override
    public void removeSyncListener(SyncType syncType, String key, Runnable listener) {
        switch (syncType) {
            case MUTEX_LOCK:
                mutexLockServer.removeSyncListener(key, listener);
                break;
            case RW_LOCK:
                rwLockServer.removeSyncListener(key, listener);
                break;
            case SEMAPHORE:
                semaphoreServer.removeSyncListener(key, listener);
                break;
            default:
                throw new IllegalArgumentException("无法识别的Sync类型：" + syncType);
        }
    }
}
