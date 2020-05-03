/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-04-29 11:56 创建
 */
package org.antframework.sync.extension;

/**
 * 服务端
 */
public interface Server {
    /**
     * 加互斥锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     * @param deadline 截止时间
     * @return null 加锁成功；否则返回需等待的时间
     */
    Long lockForMutex(String key, String lockerId, long deadline);

    /**
     * 解互斥锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     */
    void unlockForMutex(String key, String lockerId);

    /**
     * 加读锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     * @param deadline 截止时间
     * @return null 加锁成功；否则返回需等待的时间
     */
    Long lockForRead(String key, String lockerId, long deadline);

    /**
     * 解读锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     */
    void unlockForRead(String key, String lockerId);

    /**
     * 加写锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     * @param deadline 截止时间
     * @return null 加锁成功；否则返回需等待的时间
     */
    Long lockForWrite(String key, String lockerId, long deadline);

    /**
     * 解写锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     */
    void unlockForWrite(String key, String lockerId);

    /**
     * 获取信号量许可
     *
     * @param key          信号量标识
     * @param totalPermits 许可总数
     * @param newPermits   新许可数
     * @param semaphorerId 获取信号量许可者id
     * @param deadline     截止时间
     * @return null 获取成功；否则返回需等待的时间
     */
    Long acquireForSemaphore(String key, int totalPermits, int newPermits, String semaphorerId, long deadline);

    /**
     * 释放信号量许可
     *
     * @param key          信号量标识
     * @param totalPermits 许可总数
     * @param newPermits   新许可数
     * @param semaphorerId 获取信号量许可者id
     */
    void releaseForSemaphore(String key, int totalPermits, int newPermits, String semaphorerId);

    /**
     * 新增同步监听器
     *
     * @param syncType 同步类型
     * @param key      目标标识
     * @param listener 监听器
     */
    void addSyncListener(SyncType syncType, String key, Runnable listener);

    /**
     * 删除同步监听器
     *
     * @param syncType 同步类型
     * @param key      目标标识
     * @param listener 监听器
     */
    void removeSyncListener(SyncType syncType, String key, Runnable listener);

    /**
     * 同步类型
     */
    enum SyncType {
        /**
         * 互斥锁
         */
        MUTEX_LOCK,
        /**
         * 读写锁
         */
        READ_WRITE_LOCK,
        /**
         * 信号量
         */
        SEMAPHORE
    }
}
