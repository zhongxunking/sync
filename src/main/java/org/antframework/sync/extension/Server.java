/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-04-29 11:56 创建
 */
package org.antframework.sync.extension;

/**
 *
 */
public interface Server {

    Long lockForMutex(String key, String lockerId, long deadline);

    void unlockForMutex(String key, String lockerId);

    Long lockForRead(String key, String lockerId, long deadline);

    void unlockForRead(String key, String lockerId);

    Long lockForWrite(String key, String lockerId, long deadline);

    void unlockForWrite(String key, String lockerId);

    Long acquireForSemaphore(String key, int totalPermits, int newPermits, String semaphoreId, long deadline);

    void releaseForSemaphore(String key, int totalPermits, int newPermits, String semaphoreId);

    void addSyncListener(SyncType type, String key, Runnable listener);

    void removeSyncListener(SyncType type, String key, Runnable listener);

    enum SyncType {
        MUTEX_LOCK,
        READ_LOCK,
        WRITE_LOCK,
        SEMAPHORE
    }
}
