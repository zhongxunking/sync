/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-06 17:17 创建
 */
package org.antframework.sync.semaphore.annotation.support;

import lombok.AllArgsConstructor;
import org.antframework.sync.common.ExpressionEvaluator;
import org.antframework.sync.semaphore.Semaphore;
import org.antframework.sync.semaphore.SemaphoreContext;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.core.Ordered;
import org.springframework.expression.EvaluationContext;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * 信号量切面
 */
@Aspect
@AllArgsConstructor
public class SemaphoreAop implements Ordered {
    // 表达式计算器
    private final ExpressionEvaluator evaluator = new ExpressionEvaluator();
    // 信号量上下文
    private final SemaphoreContext semaphoreContext;
    // 许可总数提供者
    private final Function<String, Integer> totalPermitsSupplier;
    // 优先级
    private final int order;

    // @Semaphore切面
    @Around("@annotation(semaphoreAnnotation)")
    public Object semaphore(ProceedingJoinPoint pjp, org.antframework.sync.semaphore.annotation.Semaphore semaphoreAnnotation) throws Throwable {
        return acquirePermits(
                pjp,
                semaphoreAnnotation.condition(),
                semaphoreAnnotation.key(),
                semaphoreAnnotation.permits(),
                semaphoreAnnotation.timeout(),
                semaphoreAnnotation);
    }

    // 获取许可
    private Object acquirePermits(ProceedingJoinPoint pjp,
                                  String conditionExpression,
                                  String keyExpression,
                                  int permits,
                                  long timeout,
                                  Object annotation) throws Throwable {
        // 准备数据
        Class<?> targetClass = getTargetClass(pjp.getThis());
        Method method = getMethod(pjp, annotation);
        EvaluationContext evalContext = evaluator.getEvalContext(
                targetClass,
                pjp.getThis(),
                method,
                pjp.getArgs());
        AnnotatedElementKey methodKey = new AnnotatedElementKey(method, targetClass);
        // 计算条件
        if (StringUtils.isNotEmpty(conditionExpression)) {
            boolean condition = evaluator.evalCondition(conditionExpression, evalContext, methodKey);
            if (!condition) {
                return pjp.proceed();
            }
        }
        // 计算key
        Object key = evaluator.evalKey(keyExpression, evalContext, methodKey);
        if (key == null) {
            throw new IllegalArgumentException(String.format("key不能为null（key表达式可能有错误）：method=%s,信号量注解=%s", method, annotation));
        }
        // 获取许可
        Semaphore semaphore = semaphoreContext.getSemaphore(key.toString(), totalPermitsSupplier.apply(key.toString()));
        if (timeout < 0) {
            semaphore.acquire(permits);
        } else {
            boolean success = semaphore.tryAcquire(permits, timeout, TimeUnit.MILLISECONDS);
            if (!success) {
                throw new TimeoutException(String.format("获取许可超时：method=%s,信号量注解=%s,key=%s", method, annotation, key));
            }
        }
        try {
            return pjp.proceed();
        } finally {
            // 释放许可
            semaphore.release(permits);
        }
    }

    // 获取目标类
    private Class<?> getTargetClass(Object target) {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
        if (targetClass == null) {
            targetClass = target.getClass();
        }
        return targetClass;
    }

    // 获取方法
    private Method getMethod(ProceedingJoinPoint pjp, Object annotation) {
        Signature signature = pjp.getSignature();
        if (!(signature instanceof MethodSignature)) {
            throw new UnsupportedOperationException(String.format("注解[%s]只能在方法上使用", annotation));
        }
        return ((MethodSignature) signature).getMethod();
    }

    @Override
    public int getOrder() {
        return order;
    }
}
