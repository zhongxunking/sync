/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-26 13:34 创建
 */
package org.antframework.sync.common;

import lombok.AllArgsConstructor;
import org.antframework.sync.extension.Server;

import java.util.function.BiFunction;

/**
 * 默认的key生成器
 */
@AllArgsConstructor
public class DefaultKeyGenerator implements BiFunction<Server.SyncType, String, String> {
    /**
     * 分隔符
     */
    public static final String SEPARATOR = "::";

    // 命名空间
    private final String namespace;

    @Override
    public String apply(Server.SyncType syncType, String key) {
        return namespace + SEPARATOR + convertSyncType(syncType) + SEPARATOR + key;
    }

    // 转换同步类型
    private String convertSyncType(Server.SyncType syncType) {
        String type;
        switch (syncType) {
            case MUTEX_LOCK:
                type = "mutex-lock";
                break;
            case RW_LOCK:
                type = "rw-lock";
                break;
            case SEMAPHORE:
                type = "semaphore";
                break;
            default:
                throw new IllegalArgumentException("无法识别的Sync类型：" + syncType);
        }
        return type;
    }
}
