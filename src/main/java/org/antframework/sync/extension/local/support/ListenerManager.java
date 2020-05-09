/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-09 18:01 创建
 */
package org.antframework.sync.extension.local.support;

import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 监听器管理器
 */
@Slf4j
public class ListenerManager {
    // 标识与监听器对应关系
    private final Map<String, Set<Runnable>> keyListeners = new ConcurrentHashMap<>();

    /**
     * 新增同步监听器
     *
     * @param key      标识
     * @param listener 监听器
     */
    public void add(String key, Runnable listener) {
        keyListeners.compute(key, (k, v) -> {
            if (v == null) {
                v = new HashSet<>();
            }
            v.add(listener);
            return v;
        });
    }

    /**
     * 删除监听器
     *
     * @param key      标识
     * @param listener 监听器
     */
    public void remove(String key, Runnable listener) {
        keyListeners.computeIfPresent(key, (k, v) -> {
            v.remove(listener);
            if (v.isEmpty()) {
                v = null;
            }
            return v;
        });
    }

    /**
     * 发布事件
     *
     * @param key 标识
     */
    public void publish(String key) {
        keyListeners.computeIfPresent(key, (k, v) -> {
            v.forEach(listener -> {
                try {
                    listener.run();
                } catch (Throwable e) {
                    log.error("通知监听器出错：", e);
                }
            });
            return v;
        });
    }
}
