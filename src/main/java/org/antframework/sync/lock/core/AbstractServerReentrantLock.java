/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-04-30 15:51 创建
 */
package org.antframework.sync.lock.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.antframework.sync.common.SyncExecutor;
import org.antframework.sync.common.SyncWaiter;
import org.antframework.sync.lock.support.LockDestroyer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * 抽象基于服务端的可重入锁
 */
@AllArgsConstructor
@Slf4j
public abstract class AbstractServerReentrantLock extends AbstractReentrantLock {
    // 锁标识
    @Getter
    private final String key;
    // 加锁者id
    @Getter
    private final String lockerId;
    // Sync执行器
    private final SyncExecutor syncExecutor;

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        try {
            return super.tryLock(time, unit);
        } finally {
            onTryLockEnd();
        }
    }

    /**
     * 当trayLock方法执行结束时执行
     */
    protected abstract void onTryLockEnd();

    @Override
    protected SyncWaiter doAcquireLock(long deadline) {
        log.debug("调用server尝试加锁：lock={}", this);
        SyncWaiter waiter = lockInServer(deadline);
        if (waiter == null) {
            log.debug("调用server加锁成功：lock={}", this);
        } else {
            log.debug("调用server加锁失败，需等待：lock={},waiter={}", this, waiter);
        }
        return waiter;
    }

    /**
     * 在服务端加锁
     *
     * @param deadline 截止时间
     * @return null 加锁成功；否则失败
     */
    protected abstract SyncWaiter lockInServer(long deadline);

    @Override
    protected void doReleaseLock() {
        log.debug("调用server尝试解锁：lock={}", this);
        unlockInServer();
        log.debug("调用server解锁成功：lock={}", this);
    }

    /**
     * 在服务端解锁
     */
    protected abstract void unlockInServer();

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (getLockedTimes() > 0) {
                syncExecutor.execute(new LockDestroyer(this));
            }
        } finally {
            super.finalize();
        }
    }
}
