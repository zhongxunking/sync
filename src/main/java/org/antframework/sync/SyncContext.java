/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-03 18:12 创建
 */
package org.antframework.sync;

import lombok.Getter;
import org.antframework.sync.common.SyncExecutor;
import org.antframework.sync.extension.Server;
import org.antframework.sync.lock.LockContext;
import org.antframework.sync.semaphore.SemaphoreContext;

import java.util.function.Function;

/**
 * Sync上下文
 */
@Getter
public class SyncContext {
    // 锁上下文
    private final LockContext lockContext;
    // 信号量上下文
    private final SemaphoreContext semaphoreContext;

    public SyncContext(Function<Object, String> keyConverter, Server server, long maxWaitTime) {
        if (server == null || maxWaitTime < 0) {
            throw new IllegalArgumentException("server不能为null且maxWaitTime不能小于0");
        }
        SyncExecutor syncExecutor = new SyncExecutor();
        this.lockContext = new LockContext(keyConverter, syncExecutor, server, maxWaitTime);
        this.semaphoreContext = new SemaphoreContext(keyConverter, syncExecutor, server, maxWaitTime);
    }
}
