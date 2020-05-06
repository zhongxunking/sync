/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-03 18:45 创建
 */
package org.antframework.sync.lock.annotation;

import java.lang.annotation.*;

/**
 * 写锁
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WriteLock {
    /**
     * 加锁条件SpEL表达式
     * 表达式计算结果为bool类型的true或false。true表示满足条件，false表示不满足条件。只有满足条件才会执行加锁，默认执行加锁。
     * 表达式中可使用#参数名、#p0、#a0表示对应的方法参数，也可使用#root.targetClass、#root.target、#root.method、#root.args表示对应的类、目标对象、方法、入参。
     */
    String condition() default "";

    /**
     * 锁标识的SpEL表达式
     * 表达式计算结果可以为任意类型，但不能为null。
     * 表达式中可使用#参数名、#p0、#a0表示对应的方法参数，也可使用#root.targetClass、#root.target、#root.method、#root.args表示对应的类、目标对象、方法、入参。
     */
    String key();

    /**
     * 等待加锁的超时时间（毫秒）
     * 负数表示永远等待，直到加锁成功；0表示直接加锁，加锁失败则立即抛异常；正数表示加锁的最长等待时间，超过时间则抛异常。
     */
    long timeout() default -1;
}
