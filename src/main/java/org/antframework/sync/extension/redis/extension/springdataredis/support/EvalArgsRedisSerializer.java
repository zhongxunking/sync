/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-07 22:10 创建
 */
package org.antframework.sync.extension.redis.extension.springdataredis.support;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.lang.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * eval命令args参数的redis序列化器
 */
public final class EvalArgsRedisSerializer implements RedisSerializer<Object> {
    /**
     * 实例
     */
    public static final EvalArgsRedisSerializer INSTANCE = new EvalArgsRedisSerializer();

    private EvalArgsRedisSerializer() {
    }

    @Override
    public byte[] serialize(@Nullable Object o) throws SerializationException {
        if (o == null) {
            return null;
        }
        if (o instanceof byte[]) {
            return Arrays.copyOf((byte[]) o, ((byte[]) o).length);
        }
        return o.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Object deserialize(@Nullable byte[] bytes) throws SerializationException {
        if (bytes == null) {
            return null;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
