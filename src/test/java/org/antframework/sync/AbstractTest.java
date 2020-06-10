/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-06-09 21:28 创建
 */
package org.antframework.sync;

import org.antframework.sync.extension.local.LocalServer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 抽象测试
 */
public abstract class AbstractTest {
    /**
     * 线程数量
     */
    protected static final int AMOUNT_OF_THREAD = 10;
    /**
     * Sync上下文
     */
    protected final SyncContext syncContext = new SyncContext(new LocalServer(), 10 * 1000);
    /**
     * 线程池
     */
    protected final Executor executor = new ThreadPoolExecutor(
            AMOUNT_OF_THREAD,
            AMOUNT_OF_THREAD,
            5,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(AMOUNT_OF_THREAD));
}
