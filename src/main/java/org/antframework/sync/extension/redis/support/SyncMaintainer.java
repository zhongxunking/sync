/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-07 17:21 创建
 */
package org.antframework.sync.extension.redis.support;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * Sync维护器
 */
public class SyncMaintainer {
    // Sync持有关系
    private final Map<String, Set<String>> relations = new ConcurrentHashMap<>();

    /**
     * 新增持有关系
     *
     * @param key   Sync标识
     * @param owner 持有者
     */
    public void add(String key, String owner) {
        relations.compute(key, (k, v) -> {
            if (v == null) {
                v = new HashSet<>();
            }
            v.add(owner);
            return v;
        });
    }

    /**
     * 删除持有关系
     *
     * @param key   Sync标识
     * @param owner 持有者
     */
    public void remove(String key, String owner) {
        relations.compute(key, (k, v) -> {
            if (v != null) {
                v.remove(owner);
                if (v.isEmpty()) {
                    v = null;
                }
            }
            return v;
        });
    }

    /**
     * 获取所有Sync标识
     *
     * @return 所有Sync标识
     */
    public Set<String> keys() {
        return new HashSet<>(relations.keySet());
    }

    /**
     * 维护
     *
     * @param key        Sync标识
     * @param maintainer 维护者
     */
    public void maintain(String key, BiFunction<String, String, Boolean> maintainer) {
        relations.compute(key, (k, v) -> {
            if (v != null) {
                Iterator<String> iterator = v.iterator();
                while (iterator.hasNext()) {
                    String owner = iterator.next();
                    boolean alive = maintainer.apply(k, owner);
                    if (!alive) {
                        iterator.remove();
                    }
                }
                if (v.isEmpty()) {
                    v = null;
                }
            }
            return v;
        });
    }
}
