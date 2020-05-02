/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-02 15:48 创建
 */
package org.antframework.sync.lock.support;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 互斥资源
 */
public class MutexResource {
    // 资源
    private final Map<String, String> resources = new ConcurrentHashMap<>();

    /**
     * 查看资源的持有者
     *
     * @param resourceId 资源id
     * @return 持有者（null表示无持有者）
     */
    public String peek(String resourceId) {
        return resources.get(resourceId);
    }

    /**
     * 获取资源
     *
     * @param resourceId 资源id
     * @param owner      持有者
     * @return true 获取成功；false 获取失败
     */
    public boolean acquire(String resourceId, String owner) {
        String currentOwner = resources.computeIfAbsent(resourceId, k -> owner);
        return Objects.equals(currentOwner, owner);
    }

    /**
     * 释放资源
     *
     * @param resourceId 资源id
     * @param owner      持有者
     */
    public void release(String resourceId, String owner) {
        resources.computeIfPresent(resourceId, (k, v) -> {
            if (Objects.equals(v, owner)) {
                v = null;
            }
            return v;
        });
    }
}
