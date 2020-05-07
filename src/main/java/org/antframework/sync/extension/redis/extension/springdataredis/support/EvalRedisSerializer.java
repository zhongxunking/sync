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

import java.nio.charset.Charset;

/**
 * eval命令args和result的序列化器
 */
public class EvalRedisSerializer implements RedisSerializer<Object> {
    // key编码
    private static final Charset CHARSET = Charset.forName("utf-8");

    @Override
    public byte[] serialize(@Nullable Object o) throws SerializationException {
        if (o == null) {
            return null;
        }

        byte[] key;
        if (o instanceof byte[]) {
            key = (byte[]) o;
        } else {
            key = o.toString().getBytes(CHARSET);
        }

        return key;
    }

    @Override
    public Object deserialize(@Nullable byte[] bytes) throws SerializationException {
        if (bytes == null) {
            return null;
        }
        return new String(bytes, CHARSET);
    }
}
