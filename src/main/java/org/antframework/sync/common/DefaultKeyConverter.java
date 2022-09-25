/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-27 22:35 创建
 */
package org.antframework.sync.common;

import java.util.function.Function;

/**
 * 默认的key转换器
 */
public class DefaultKeyConverter implements Function<Object, String> {
    @Override
    public String apply(Object key) {
        if (key == null) {
            return null;
        }
        return key.toString();
    }
}
