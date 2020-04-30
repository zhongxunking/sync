/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-04-29 15:49 创建
 */
package org.antframework.sync.lock.core;

import lombok.Getter;
import org.antframework.sync.core.SyncWaiter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * 抽象可重入锁
 */
@Getter
public abstract class AbstractReentrantLock implements Lock {
    // 被加锁的次数
    private int lockedTimes = 0;

    @Override
    public void lock() {
        try {
            lockInterruptibly();
        } catch (InterruptedException e) {
            ExceptionUtils.rethrow(e);
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        boolean success;
        do {
            success = tryLock(1, TimeUnit.HOURS);
        } while (!success);
    }

    @Override
    public boolean tryLock() {
        try {
            return tryLock(0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(time);
        SyncWaiter waiter = acquireLock(deadline);
        while (waiter != null) {
            long timeout = deadline - System.currentTimeMillis();
            if (timeout <= 0) {
                return false;
            }
            waiter.waitSync(timeout, TimeUnit.MILLISECONDS);
            waiter = acquireLock(deadline);
        }
        return true;
    }

    // 尝试获取锁（返回null表示加锁成功；否则失败）
    private synchronized SyncWaiter acquireLock(long deadline) {
        SyncWaiter waiter = null;
        if (lockedTimes <= 0) {
            waiter = doAcquireLock(deadline);
        }
        if (waiter == null) {
            lockedTimes++;
        }
        return waiter;
    }

    /**
     * 获取锁
     *
     * @param deadline 截止时间
     * @return null 表示加锁成功；否则失败
     */
    protected abstract SyncWaiter doAcquireLock(long deadline);

    @Override
    public void unlock() {
        releaseLock();
    }

    // 尝试释放锁（返回true表示锁已被真正释放，否则锁未被真正释放）
    private synchronized boolean releaseLock() {
        if (lockedTimes > 0) {
            lockedTimes--;
            if (lockedTimes <= 0) {
                doReleaseLock();
            }
        }
        return lockedTimes <= 0;
    }

    /**
     * 释放锁
     */
    protected abstract void doReleaseLock();
}
