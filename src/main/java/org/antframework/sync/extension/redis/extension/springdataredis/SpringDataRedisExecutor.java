/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-07 22:08 创建
 */
package org.antframework.sync.extension.redis.extension.springdataredis;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antframework.sync.extension.redis.extension.RedisExecutor;
import org.antframework.sync.extension.redis.extension.springdataredis.support.EvalArgsRedisSerializer;
import org.antframework.sync.extension.redis.extension.springdataredis.support.RedisListenerContainer;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于spring-data-redis的redis执行器
 */
@Slf4j
public class SpringDataRedisExecutor implements RedisExecutor {
    // 监听器与redis消息监听器的映射关系
    private final Map<MessageListenerKey, MessageListener> listeners = new ConcurrentHashMap<>();
    // redisTemplate
    private final RedisTemplate<String, byte[]> redisTemplate;
    // redis监听器容器
    private final RedisListenerContainer listenerContainer;

    public SpringDataRedisExecutor(RedisConnectionFactory redisConnectionFactory) {
        if (redisConnectionFactory == null) {
            throw new IllegalArgumentException("redisConnectionFactory不能为null");
        }
        this.redisTemplate = new RedisTemplate<>();
        this.redisTemplate.setConnectionFactory(redisConnectionFactory);
        this.redisTemplate.setKeySerializer(RedisSerializer.string());
        this.redisTemplate.setValueSerializer(RedisSerializer.byteArray());
        this.redisTemplate.setHashKeySerializer(RedisSerializer.string());
        this.redisTemplate.setHashValueSerializer(RedisSerializer.byteArray());
        this.redisTemplate.afterPropertiesSet();
        this.listenerContainer = new RedisListenerContainer(redisConnectionFactory);
    }

    @Override
    public Object encodeScript(String script, Class<?> resultType) {
        return new DefaultRedisScript<>(script, resultType);
    }

    @Override
    public <T> T eval(Object encodedScript, List<String> keys, List<Object> args) {
        return (T) redisTemplate.execute(
                (DefaultRedisScript) encodedScript,
                EvalArgsRedisSerializer.INSTANCE,
                RedisSerializer.byteArray(),
                keys,
                args.toArray());
    }

    @Override
    public void addMessageListener(String channel, Runnable listener) {
        MessageListenerKey key = new MessageListenerKey(channel, listener);
        listeners.computeIfAbsent(key, k -> {
            MessageListener messageListener = (message, pattern) -> {
                try {
                    listener.run();
                } catch (Throwable e) {
                    log.error("处理Redis消息失败", e);
                }
            };
            listenerContainer.addMessageListener(messageListener, new ChannelTopic(channel));
            return messageListener;
        });
    }

    @Override
    public void removeMessageListener(String channel, Runnable listener) {
        MessageListenerKey key = new MessageListenerKey(channel, listener);
        listeners.computeIfPresent(key, (k, v) -> {
            listenerContainer.removeMessageListener(v, new ChannelTopic(channel));
            return null;
        });
    }

    /**
     * 消息监听器key
     */
    @AllArgsConstructor
    private final static class MessageListenerKey {
        // 通道
        private final String channel;
        // 监听器
        private final Runnable listener;

        @Override
        public int hashCode() {
            return Objects.hash(channel, listener);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MessageListenerKey)) {
                return false;
            }
            MessageListenerKey other = (MessageListenerKey) obj;
            return Objects.equals(channel, other.channel) && Objects.equals(listener, other.listener);
        }
    }
}
