/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-04-30 16:05 创建
 */
package org.antframework.sync.lock.support;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antframework.sync.lock.core.AbstractReentrantLock;

/**
 * 锁销毁器
 */
@AllArgsConstructor
@Slf4j
public class LockDestroyer implements Runnable {
    // 锁
    private final AbstractReentrantLock lock;

    @Override
    public void run() {
        log.error("存在未解锁就被弃用的锁（{}），请检查代码中是否有bug。现强制解锁！", lock);
        try {
            int lockedTimes = lock.getLockedTimes();
            for (int i = 0; i < lockedTimes; i++) {
                lock.unlock();
            }
        } catch (Throwable e) {
            log.error("强制解锁出错（{}）", lock, e);
        }
    }
}
