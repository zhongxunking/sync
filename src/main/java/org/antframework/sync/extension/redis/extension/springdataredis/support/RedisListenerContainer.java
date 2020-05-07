/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-07 22:12 创建
 */
package org.antframework.sync.extension.redis.extension.springdataredis.support;

import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;

/**
 * redis监听器容器
 */
public class RedisListenerContainer {
    // 容器
    private final RedisMessageListenerContainer container;

    public RedisListenerContainer(RedisConnectionFactory connectionFactory) {
        this.container = new RedisMessageListenerContainer();
        this.container.setConnectionFactory(connectionFactory);
        this.container.afterPropertiesSet();
        this.container.start();
        // 添加空监听器，防止容器报错
        this.container.addMessageListener((message, pattern) -> {
        }, new ChannelTopic("sync"));
    }

    /**
     * 新增消息监听器
     *
     * @param listener 监听器
     * @param topic    主题
     */
    public synchronized void addMessageListener(MessageListener listener, Topic topic) {
        container.addMessageListener(listener, topic);
    }

    /**
     * 删除消息监听器
     *
     * @param listener 监听器
     * @param topic    主题
     */
    public synchronized void removeMessageListener(MessageListener listener, Topic topic) {
        container.removeMessageListener(listener, topic);
    }
}
