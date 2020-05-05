/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-05 14:00 创建
 */
package org.antframework.sync.semaphore.support;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 有限资源
 */
public class FiniteResource {
    // 资源
    private final Map<String, ResourceOwner> resources = new ConcurrentHashMap<>();

    /**
     * 查看持有者持有的资源数量
     *
     * @param resourceId 资源id
     * @param owner      持有者
     * @return 资源数量
     */
    public int peek(String resourceId, String owner) {
        AtomicInteger amount = new AtomicInteger(0);
        visitTemplate(resourceId, resourceOwner -> amount.set(resourceOwner.peek(owner)));
        return amount.get();
    }

    /**
     * 获取资源
     *
     * @param resourceId  资源id
     * @param owner       持有者
     * @param newAmount   新数量
     * @param totalAmount 总数量
     * @return
     */
    public boolean acquire(String resourceId, String owner, int newAmount, int totalAmount) {
        AtomicBoolean success = new AtomicBoolean(false);
        visitTemplate(resourceId, resourceOwner -> success.set(resourceOwner.acquire(owner, newAmount, totalAmount)));
        return success.get();
    }

    /**
     * 释放资源
     *
     * @param resourceId 资源id
     * @param owner      持有者
     * @param newAmount  新数量
     */
    public void release(String resourceId, String owner, int newAmount) {
        visitTemplate(resourceId, resourceOwner -> resourceOwner.release(owner, newAmount));
    }

    // 访问模版方法
    private void visitTemplate(String resourceId, Consumer<ResourceOwner> visitor) {
        resources.compute(resourceId, (k, v) -> {
            if (v == null) {
                v = new ResourceOwner();
            }
            visitor.accept(v);
            if (v.isEmpty()) {
                v = null;
            }
            return v;
        });
    }

    /**
     * 资源持有者
     */
    private static class ResourceOwner {
        // 所有持有者
        private final Map<String, Integer> owners = new HashMap<>();

        // 查看持有者持有的资源数量
        int peek(String owner) {
            Integer amount = owners.get(owner);
            if (amount == null) {
                amount = 0;
            }
            return amount;
        }

        // 获取资源
        boolean acquire(String owner, int newAmount, int totalAmount) {
            int allAmount = newAmount;
            for (Map.Entry<String, Integer> entry : owners.entrySet()) {
                if (!Objects.equals(entry.getKey(), owner)) {
                    allAmount += entry.getValue();
                }
            }
            boolean success = false;
            if (allAmount <= totalAmount) {
                updateAmount(owner, newAmount);
                success = true;
            }
            return success;
        }

        // 释放资源
        void release(String owner, int newAmount) {
            updateAmount(owner, newAmount);
        }

        // 更新持有的资源数量
        private void updateAmount(String owner, int newAmount) {
            if (newAmount > 0) {
                owners.put(owner, newAmount);
            } else {
                owners.remove(owner);
            }
        }

        // 是否不存在持有者
        boolean isEmpty() {
            return owners.isEmpty();
        }
    }
}
