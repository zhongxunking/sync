/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-04 11:38 创建
 */
package org.antframework.sync.semaphore.core;

import lombok.Getter;
import org.antframework.sync.common.SyncWaiter;
import org.antframework.sync.semaphore.Semaphore;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.concurrent.TimeUnit;

/**
 * 抽象信号量
 */
@Getter
public abstract class AbstractSemaphore implements Semaphore {
    // 获取到的许可
    private int acquiredPermits = 0;

    @Override
    public void acquire() throws InterruptedException {
        acquire(1);
    }

    @Override
    public void acquire(int permits) throws InterruptedException {
        boolean success;
        do {
            success = tryAcquire(permits, 1, TimeUnit.HOURS);
        } while (!success);
    }

    @Override
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    @Override
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return tryAcquire(1, timeout, unit);
    }

    @Override
    public boolean tryAcquire(int permits) {
        try {
            return tryAcquire(permits, 0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    @Override
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws InterruptedException {
        if (permits < 0) {
            throw new IllegalArgumentException("permits必须大于或等于0");
        }
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        SyncWaiter waiter = acquirePermits(permits, deadline);
        while (waiter != null) {
            long time = deadline - System.currentTimeMillis();
            if (time <= 0) {
                return false;
            }
            waiter.waitSync(time);
            waiter = acquirePermits(permits, deadline);
        }
        return true;
    }

    // 获取许可（返回null表示获取成功；否则失败）
    private synchronized SyncWaiter acquirePermits(int permits, long deadline) {
        int newPermits = acquiredPermits + permits;
        SyncWaiter waiter = doAcquire(newPermits, deadline);
        if (waiter == null) {
            acquiredPermits = newPermits;
        }
        return waiter;
    }

    /**
     * 执行获取许可
     *
     * @param newPermits 新的许可数
     * @param deadline   截止时间
     * @return null 获取成功；否则获取失败
     */
    protected abstract SyncWaiter doAcquire(int newPermits, long deadline);

    @Override
    public void release() {
        release(1);
    }

    @Override
    public void release(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException("permits必须大于或等于0");
        }
        releasePermits(permits);
    }

    // 释放许可
    private synchronized void releasePermits(int permits) {
        acquiredPermits = Math.max(acquiredPermits - permits, 0);
        doRelease(acquiredPermits);
    }

    /**
     * 执行释放许可
     *
     * @param newPermits 新的许可数
     */
    protected abstract void doRelease(int newPermits);
}
