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
import org.antframework.sync.core.SyncExecutor;
import org.antframework.sync.core.SyncWaiter;
import org.antframework.sync.lock.support.LockDestroyer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 *
 */
@AllArgsConstructor
@Slf4j
public abstract class AbstractServerReentrantLock extends AbstractReentrantLock {
    @Getter
    private final String lockerId;

    @Getter
    private final String key;

    private final SyncExecutor syncExecutor;

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        try {
            return super.tryLock(time, unit);
        } finally {
            onTryLockEnd();
        }
    }

    protected abstract void onTryLockEnd();

    @Override
    protected SyncWaiter doAcquireLock(long deadline) {
        log.debug("调用server加锁：{}", this);
        SyncWaiter waiter = lockInServer(deadline);
        if (waiter != null) {
            log.debug("调用server加锁失败，需等待：{}", waiter);
        }
        return waiter;
    }

    protected abstract SyncWaiter lockInServer(long deadline);

    @Override
    protected void doReleaseLock() {
        unlockInServer();
        log.debug("调用server解锁：{}", this);
    }

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
