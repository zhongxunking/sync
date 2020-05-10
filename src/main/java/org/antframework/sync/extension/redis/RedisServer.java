/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-07 14:27 创建
 */
package org.antframework.sync.extension.redis;

import lombok.extern.slf4j.Slf4j;
import org.antframework.sync.extension.Server;
import org.antframework.sync.extension.redis.extension.RedisExecutor;
import org.antframework.sync.extension.redis.support.RedisMutexLockServer;
import org.antframework.sync.extension.redis.support.RedisRWLockServer;
import org.antframework.sync.extension.redis.support.RedisSemaphoreServer;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * redis服务端
 */
@Slf4j
public class RedisServer implements Server {
    // 定时触发器
    private final Timer timer = new Timer("RedisServer-sync-maintainer", true);
    // redis执行器
    private final RedisExecutor redisExecutor;
    // 互斥锁服务端
    private final RedisMutexLockServer mutexLockServer;
    // 读写锁服务端
    private final RedisRWLockServer rwLockServer;
    // 信号量服务端
    private final RedisSemaphoreServer semaphoreServer;

    public RedisServer(RedisExecutor redisExecutor, long liveTime) {
        Executor maintainExecutor = new ThreadPoolExecutor(
                1,
                10,
                5,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1024),
                new ThreadPoolExecutor.CallerRunsPolicy());
        this.redisExecutor = redisExecutor;
        this.mutexLockServer = new RedisMutexLockServer(redisExecutor, liveTime, maintainExecutor);
        this.rwLockServer = new RedisRWLockServer(redisExecutor, liveTime, maintainExecutor);
        this.semaphoreServer = new RedisSemaphoreServer(redisExecutor, liveTime, maintainExecutor);
        this.timer.schedule(new MaintainTask(), liveTime / 10, liveTime / 10);
    }

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
        semaphoreServer.release(key, semaphorerId, newPermits, totalPermits);
    }

    @Override
    public void addSyncListener(SyncType syncType, String key, Runnable listener) {
        String channel = computeSyncChannel(syncType, key);
        redisExecutor.addMessageListener(channel, listener);
    }

    @Override
    public void removeSyncListener(SyncType syncType, String key, Runnable listener) {
        String channel = computeSyncChannel(syncType, key);
        redisExecutor.removeMessageListener(channel, listener);
    }

    // 计算同步通道
    private String computeSyncChannel(SyncType syncType, String key) {
        String channel;
        switch (syncType) {
            case MUTEX_LOCK:
                channel = mutexLockServer.computeSyncChannel(key);
                break;
            case RW_LOCK:
                channel = rwLockServer.computeSyncChannel(key);
                break;
            case SEMAPHORE:
                channel = semaphoreServer.computeSyncChannel(key);
                break;
            default:
                throw new IllegalArgumentException("无法识别的Sync类型：" + syncType);
        }
        return channel;
    }

    // 维护任务
    private class MaintainTask extends TimerTask {
        @Override
        public void run() {
            try {
                mutexLockServer.maintain();
                rwLockServer.maintain();
                semaphoreServer.maintain();
            } catch (Throwable e) {
                log.error("定时维护互斥锁、读写锁、信号量在redis中的有效期出错：", e);
            }
        }
    }
}
