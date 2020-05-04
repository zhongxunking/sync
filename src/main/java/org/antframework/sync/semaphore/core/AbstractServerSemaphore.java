/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-04 12:34 创建
 */
package org.antframework.sync.semaphore.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.antframework.sync.common.SyncExecutor;
import org.antframework.sync.common.SyncWaiter;
import org.antframework.sync.semaphore.support.SemaphoreDestroyer;

import java.util.concurrent.TimeUnit;

/**
 * 抽象基于服务端的信号量
 */
@AllArgsConstructor
@Slf4j
public abstract class AbstractServerSemaphore extends AbstractSemaphore {
    // 信号量标识
    @Getter
    private final String key;
    // 获取信号量许可者id
    @Getter
    private final String semaphorerId;
    // 许可总数
    @Getter
    private final int totalPermits;
    // Sync执行器
    private final SyncExecutor syncExecutor;

    @Override
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws InterruptedException {
        try {
            return super.tryAcquire(permits, timeout, unit);
        } finally {
            onTryAcquireEnd();
        }
    }

    /**
     * 当tryAcquire方法执行结束时执行
     */
    protected abstract void onTryAcquireEnd();

    @Override
    protected SyncWaiter doAcquire(int newPermits, long deadline) {
        if (newPermits > totalPermits) {
            throw new IllegalArgumentException(String.format("获取的信号量许可[%s]不能超过许可总数[%s]", newPermits, totalPermits));
        }
        log.debug("调用server获取许可：semaphore={},newPermits={}", this, newPermits);
        SyncWaiter waiter = acquireInServer(newPermits, deadline);
        if (waiter != null) {
            log.debug("调用server获取许可失败，需等待：waiter={}", waiter);
        }
        return waiter;
    }

    /**
     * 在服务端获取许可
     *
     * @param newPermits 新的许可数
     * @param deadline   截止时间
     * @return null 获取成功；否则获取失败
     */
    protected abstract SyncWaiter acquireInServer(int newPermits, long deadline);

    @Override
    protected void doRelease(int newPermits) {
        releaseInServer(newPermits);
        log.debug("调用server释放许可：semaphore={}", this);
    }

    /**
     * 在服务端释放许可
     *
     * @param newPermits 新的许可数
     */
    protected abstract void releaseInServer(int newPermits);

    @Override
    protected void finalize() throws Throwable {
        try {
            if (getAcquiredPermits() > 0) {
                syncExecutor.execute(new SemaphoreDestroyer(this));
            }
        } finally {
            super.finalize();
        }
    }
}
