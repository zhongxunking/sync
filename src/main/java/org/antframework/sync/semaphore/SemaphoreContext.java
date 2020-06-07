/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-05 16:34 创建
 */
package org.antframework.sync.semaphore;

import org.antframework.sync.common.SyncExecutor;
import org.antframework.sync.common.SyncUtils;
import org.antframework.sync.extension.Server;
import org.antframework.sync.semaphore.core.DefaultServerSemaphore;
import org.antframework.sync.semaphore.support.SemaphoreServer;

/**
 * 信号量上下文
 */
public class SemaphoreContext {
    // Sync执行器
    private final SyncExecutor syncExecutor;
    // 信号量服务端
    private final SemaphoreServer semaphoreServer;

    public SemaphoreContext(SyncExecutor syncExecutor, Server server, long maxWaitTime) {
        this.syncExecutor = syncExecutor;
        this.semaphoreServer = new SemaphoreServer(server, maxWaitTime);
    }

    /**
     * 获取信号量
     *
     * @param key          信号量标识
     * @param totalPermits 许可总数
     * @return 信号量
     */
    public Semaphore getSemaphore(String key, int totalPermits) {
        if (key == null || totalPermits < 0) {
            throw new IllegalArgumentException("key不能为null且totalPermits不能小于0");
        }
        return new DefaultServerSemaphore(key, SyncUtils.newId(), totalPermits, syncExecutor, semaphoreServer);
    }
}
