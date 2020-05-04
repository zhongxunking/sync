/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-04 12:59 创建
 */
package org.antframework.sync.semaphore.support;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antframework.sync.semaphore.core.AbstractSemaphore;

/**
 * 信号量销毁器
 */
@AllArgsConstructor
@Slf4j
public class SemaphoreDestroyer implements Runnable {
    // 信号量
    private final AbstractSemaphore semaphore;

    @Override
    public void run() {
        log.error("存在未释放就被弃用的信号量（{}），请检查代码中是否有bug。现强制释放！", semaphore);
        try {
            int acquiredPermits = semaphore.getAcquiredPermits();
            semaphore.release(acquiredPermits);
        } catch (Throwable e) {
            log.error("强制释放信号量出错（{}）", semaphore, e);
        }
    }
}
