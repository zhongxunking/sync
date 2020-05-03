/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-03 17:53 创建
 */
package org.antframework.sync.lock.core;

import lombok.Getter;
import org.antframework.sync.common.SyncExecutor;
import org.antframework.sync.lock.support.RWLockServer;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * 基于服务端的可重入读写锁
 */
public class ServerReentrantRWLock implements ReadWriteLock {
    // 锁标识
    @Getter
    private final String key;
    // 加锁者id
    @Getter
    private final String lockerId;
    // 读锁
    private final Lock readLock;
    // 写锁
    private final Lock writeLock;

    public ServerReentrantRWLock(String key, String lockerId, SyncExecutor syncExecutor, RWLockServer server) {
        this.key = key;
        this.lockerId = lockerId;
        this.readLock = new ServerReentrantReadLock(key, lockerId, syncExecutor, server);
        this.writeLock = new ServerReentrantWriteLock(key, lockerId, syncExecutor, server);
    }

    @Override
    public Lock readLock() {
        return readLock;
    }

    @Override
    public Lock writeLock() {
        return writeLock;
    }
}
