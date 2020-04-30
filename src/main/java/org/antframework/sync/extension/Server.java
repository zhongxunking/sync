/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-04-29 11:56 创建
 */
package org.antframework.sync.extension;

import java.util.concurrent.TimeUnit;

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

    boolean waitSync(SyncType type, String key, long timeout, TimeUnit unit);

    void removeWaiter(SyncType type, String key);

    enum SyncType {
        MUTEX_LOCK,
        READ_LOCK,
        WRITE_LOCK,
        SEMAPHORE
    }
}
