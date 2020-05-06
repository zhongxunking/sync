/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-06 17:11 创建
 */
package org.antframework.sync.semaphore.annotation;

import java.lang.annotation.*;

/**
 * 信号量
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Semaphore {
    /**
     * 获取许可条件SpEL表达式
     * 表达式计算结果为bool类型的true或false。true表示满足条件，false表示不满足条件。只有满足条件才会获取许可，默认执行获取许可。
     * 表达式中可使用#参数名、#p0、#a0表示对应的方法参数，也可使用#root.targetClass、#root.target、#root.method、#root.args表示对应的类、目标对象、方法、入参。
     */
    String condition() default "";

    /**
     * 信号量标识的SpEL表达式
     * 表达式计算结果可以为任意类型，但不能为null。
     * 表达式中可使用#参数名、#p0、#a0表示对应的方法参数，也可使用#root.targetClass、#root.target、#root.method、#root.args表示对应的类、目标对象、方法、入参。
     */
    String key();

    /**
     * 获取的许可数量
     */
    int permits();

    /**
     * 等待获取许可成功的超时时间（毫秒）
     * 负数表示永远等待，直到获取成功；0表示直接获取，获取失败则立即抛异常；正数表示获取的最长等待时间，超过时间则抛异常。
     */
    long timeout() default -1;
}
