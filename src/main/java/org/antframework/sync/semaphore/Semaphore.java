/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-04 11:35 创建
 */
package org.antframework.sync.semaphore;

import java.util.concurrent.TimeUnit;

/**
 * 信号量
 */
public interface Semaphore {
    /**
     * 获取一个许可（阻塞）
     *
     * @throws InterruptedException 获取过程中发生中断
     */
    void acquire() throws InterruptedException;

    /**
     * 获取许可（阻塞）
     *
     * @param permits 许可数量
     * @throws InterruptedException 获取过程中发生中断
     */
    void acquire(int permits) throws InterruptedException;

    /**
     * 尝试获取一个许可（立即返回）
     *
     * @return true 获取成功；false 获取失败
     */
    boolean tryAcquire();

    /**
     * 尝试获取一个许可（阻塞）
     *
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true 获取成功；false 获取失败
     * @throws InterruptedException 获取过程中发生中断
     */
    boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * 尝试获取许可（立即返回）
     *
     * @param permits 许可数量
     * @return true 获取成功；false 获取失败
     */
    boolean tryAcquire(int permits);

    /**
     * 尝试获取许可（阻塞）
     *
     * @param permits 许可数量
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true 获取成功；false 获取失败
     * @throws InterruptedException 获取过程中发生中断
     */
    boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * 释放一个许可
     */
    void release();

    /**
     * 释放许可
     *
     * @param permits 许可数量
     */
    void release(int permits);
}
