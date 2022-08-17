/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-07 18:04 创建
 */
package org.antframework.sync.extension.redis.extension;

import java.util.List;

/**
 * redis执行器
 */
public interface RedisExecutor {
    /**
     * 执行脚本
     *
     * @param script     脚本
     * @param keys       脚本中的KEYS
     * @param args       脚本中的ARGV
     * @param resultType 返回值类型
     * @param <T>        返回值类型
     * @return 脚本返回值
     */
    <T> T eval(String script, List<String> keys, List<Object> args, Class<T> resultType);

    /**
     * 新增消息监听器
     *
     * @param channel  通道
     * @param listener 监听器
     */
    void addMessageListener(String channel, Runnable listener);

    /**
     * 删除消息监听器
     *
     * @param channel  通道
     * @param listener 监听器
     */
    void removeMessageListener(String channel, Runnable listener);
}
