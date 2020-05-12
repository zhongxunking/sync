/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-04-29 16:09 创建
 */
package org.antframework.sync.common;

/**
 * 同步等待器
 */
public interface SyncWaiter {
    /**
     * 等待同步
     *
     * @param timeout 等待超时时间（毫秒）
     * @return true 等到同步; false 等待超时
     */
    boolean waitSync(long timeout) throws InterruptedException;
}
