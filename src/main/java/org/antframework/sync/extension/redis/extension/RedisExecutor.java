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
     * 编码脚本
     *
     * @param script     脚本文本
     * @param resultType 返回值类型
     * @return 编码后的脚本
     */
    Object encodeScript(String script, Class<?> resultType);

    /**
     * 执行脚本
     *
     * @param encodedScript 编码后的脚本
     * @param keys          脚本中的KEYS
     * @param args          脚本中的ARGV
     * @param <T>           返回值类型
     * @return 脚本返回值
     */
    <T> T eval(Object encodedScript, List<String> keys, List<Object> args);

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
