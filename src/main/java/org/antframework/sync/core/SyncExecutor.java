/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-04-30 15:56 创建
 */
package org.antframework.sync.core;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Sync执行器
 */
public class SyncExecutor {
    // 线程池
    private final Executor executor = new ThreadPoolExecutor(
            0,
            10,
            5,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1024),
            new ThreadPoolExecutor.DiscardPolicy());

    /**
     * 执行
     *
     * @param task 任务
     */
    public void execute(Runnable task) {
        executor.execute(task);
    }
}
