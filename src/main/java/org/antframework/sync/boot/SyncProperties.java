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
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Sync配置
 */
@ConfigurationProperties("ant.sync")
@Validated
@Getter
@Setter
public class SyncProperties {
    /**
     * 选填：等待同步消息的最长时间（毫秒，默认为30秒）
     */
    @Min(0)
    private long maxWaitTime = 30 * 1000;
    /**
     * 选填：服务端类型（默认为redis）
     */
    @NotNull
    private ServerType serverType = ServerType.REDIS;
    /**
     * redis配置
     */
    @NotNull
    @Valid
    private Redis redis = new Redis();

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
     * 基于redis实现的服务端配置
     */
    @Getter
    @Setter
    public static class Redis {
        /**
         * 选填：发生异常时redis中数据的存活时长（毫秒，默认为10分钟）
         */
        @Min(1)
        private long liveTime = 10 * 60 * 1000;
    }
}
