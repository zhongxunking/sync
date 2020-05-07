/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-07 22:08 创建
 */
package org.antframework.sync.extension.redis.extension.springdataredis;

import org.antframework.sync.extension.redis.extension.RedisExecutor;
import org.antframework.sync.extension.redis.extension.springdataredis.support.EvalRedisSerializer;
import org.antframework.sync.extension.redis.extension.springdataredis.support.RedisListenerContainer;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于spring-data-redis的redis执行器
 */
public class SpringDataRedisExecutor implements RedisExecutor {
    // redis序列化器
    private static final RedisSerializer<Object> REDIS_SERIALIZER = new EvalRedisSerializer();
    // 监听器与redis消息监听器的映射关系
    private final Map<Runnable, MessageListener> listenerMap = new ConcurrentHashMap<>();
    // redisTemplate
    private final RedisTemplate<Object, Object> redisTemplate;
    // redis监听器容器
    private final RedisListenerContainer listenerContainer;

    public SpringDataRedisExecutor(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.listenerContainer = new RedisListenerContainer(redisTemplate.getConnectionFactory());
    }

    @Override
    public <T> T eval(String script, List<Object> keys, List<Object> args, Class<T> resultType) {
        return redisTemplate.execute(
                new DefaultRedisScript<>(script, resultType),
                REDIS_SERIALIZER,
                (RedisSerializer<T>) REDIS_SERIALIZER,
                keys,
                args.toArray());
    }

    @Override
    public boolean expire(String key, long timeout, TimeUnit unit) {
        Boolean success = redisTemplate.expire(key, timeout, unit);
        if (success == null) {
            success = true;
        }
        return success;
    }

    @Override
    public void addMessageListener(String channel, Runnable listener) {
        listenerMap.computeIfAbsent(listener, k -> {
            MessageListener messageListener = (message, pattern) -> listener.run();
            listenerContainer.addMessageListener(messageListener, new ChannelTopic(channel));
            return messageListener;
        });
    }

    @Override
    public void removeMessageListener(String channel, Runnable listener) {
        listenerMap.computeIfPresent(listener, (k, v) -> {
            listenerContainer.removeMessageListener(v, new ChannelTopic(channel));
            return null;
        });
    }
}
