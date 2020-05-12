/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-12 09:41 创建
 */
package org.antframework.sync.boot;

import org.antframework.sync.SyncContext;
import org.antframework.sync.extension.Server;
import org.antframework.sync.extension.local.LocalServer;
import org.antframework.sync.extension.redis.RedisServer;
import org.antframework.sync.extension.redis.extension.RedisExecutor;
import org.antframework.sync.extension.redis.extension.springdataredis.SpringDataRedisExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Sync自动配置
 */
@Configuration
@ConditionalOnMissingBean(SyncContext.class)
@EnableConfigurationProperties(SyncProperties.class)
public class SyncAutoConfiguration {
    // Sync上下文
    @Bean(name = "org.antframework.sync.SyncContext")
    public SyncContext syncContext(Server server, SyncProperties properties) {
        return new SyncContext(server, properties.getMaxWaitTime());
    }

    /**
     * server配置
     */
    @Configuration
    @ConditionalOnMissingBean(Server.class)
    public static class ServerConfiguration {
        /**
         * 本地版server配置
         */
        @Configuration
        @ConditionalOnProperty(name = "ant.sync.server-type", havingValue = "local")
        public static class LocalServerConfiguration {
            // server
            @Bean(name = "org.antframework.sync.extension.Server")
            public Server server() {
                return new LocalServer();
            }
        }

        /**
         * redis版server配置
         */
        @Configuration
        @ConditionalOnProperty(name = "ant.sync.server-type", havingValue = "redis", matchIfMissing = true)
        public static class RedisServerConfiguration {
            // server
            @Bean(name = "org.antframework.sync.extension.Server")
            public Server server(RedisExecutor redisExecutor, SyncProperties properties) {
                return new RedisServer(redisExecutor, properties.getRedis().getLiveTime());
            }

            /**
             * redis执行器配置
             */
            @Configuration
            @ConditionalOnMissingBean(RedisExecutor.class)
            public static class RedisExecutorConfiguration {
                // redis执行器（默认使用spring-data-redis）
                @Bean(name = "org.antframework.sync.extension.redis.extension.RedisExecutor")
                public RedisExecutor redisExecutor(RedisTemplate<Object, Object> redisTemplate) {
                    return new SpringDataRedisExecutor(redisTemplate);
                }
            }
        }
    }
}
