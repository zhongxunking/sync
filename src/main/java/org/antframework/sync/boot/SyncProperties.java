/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-12 10:47 创建
 */
package org.antframework.sync.boot;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 * Sync配置
 */
@ConfigurationProperties("ant.sync")
@Validated
@Getter
@Setter
public class SyncProperties {
    /**
     * 是否启用Sync的key
     */
    public static final String ENABLE_KEY = "ant.sync.enable";
    /**
     * key转换器的bean名称
     */
    public static final String KEY_CONVERTER_BEAN_NAME = "org.antframework.sync.keyConverter";
    /**
     * key生成器的bean名称
     */
    public static final String KEY_GENERATOR_BEAN_NAME = "org.antframework.sync.keyGenerator";

    /**
     * 选填：是否启用Sync（true为启用，false为不启用；默认启用）
     */
    private boolean enable = true;
    /**
     * 选填：命名空间（默认为spring.application.name对应的值）
     */
    private String namespace = null;
    /**
     * 选填：等待同步消息的最长时间（毫秒，默认为10秒）
     */
    @Min(0)
    private long maxWaitTime = 10 * 1000;
    /**
     * 选填：服务端类型（默认为redis）
     */
    @NotNull
    private ServerType serverType = ServerType.REDIS;
    /**
     * Redis配置
     */
    @NotNull
    @Valid
    private Redis redis = new Redis();
    /**
     * 信号量配置
     */
    @NotNull
    @Valid
    private Semaphore semaphore = new Semaphore();
    /**
     * 选填：@Lock、@ReadLock、@WriteLock、@Semaphore的AOP执行的优先级（默认为Ordered.LOWEST_PRECEDENCE - 10，默认比@Transactional先执行）
     */
    private int aopOrder = Ordered.LOWEST_PRECEDENCE - 10;

    /**
     * 服务端类型
     */
    enum ServerType {
        /**
         * 本地
         */
        LOCAL,
        /**
         * redis
         */
        REDIS
    }

    /**
     * Redis配置
     */
    @Getter
    @Setter
    public static class Redis {
        /**
         * 选填：发生异常时Redis中数据的存活时长（毫秒，默认为10分钟）
         */
        @Min(1)
        private long liveTime = 10 * 60 * 1000;
    }

    /**
     * 信号量配置
     */
    @Getter
    @Setter
    public static class Semaphore {
        /**
         * key对应的总许可数
         */
        @NotNull
        private Map<String, Integer> keyTotalPermits = new HashMap<>();
    }
}
