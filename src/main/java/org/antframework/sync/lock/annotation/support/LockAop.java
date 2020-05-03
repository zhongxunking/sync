/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-03 18:46 创建
 */
package org.antframework.sync.lock.annotation.support;

import lombok.AllArgsConstructor;
import org.antframework.sync.common.ExpressionEvaluator;
import org.antframework.sync.lock.LockContext;
import org.antframework.sync.lock.annotation.ReadLock;
import org.antframework.sync.lock.annotation.WriteLock;
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
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

/**
 * 锁切面
 */
@Aspect
@AllArgsConstructor
public class LockAop implements Ordered {
    // 锁表达式计算器
    private final ExpressionEvaluator evaluator = new ExpressionEvaluator();
    // 锁上下文
    private final LockContext lockContext;
    // 优先级
    private final int order;

    // @Lock切面
    @Around("@annotation(lockAnnotation)")
    public Object lock(ProceedingJoinPoint pjp, org.antframework.sync.lock.annotation.Lock lockAnnotation) throws Throwable {
        return lock(
                pjp,
                lockAnnotation.condition(),
                lockAnnotation.key(),
                lockAnnotation.timeout(),
                lockContext::getLock,
                lockAnnotation);
    }

    // @ReadLock切面
    @Around("@annotation(readLockAnnotation)")
    public Object readLock(ProceedingJoinPoint pjp, ReadLock readLockAnnotation) throws Throwable {
        return lock(
                pjp,
                readLockAnnotation.condition(),
                readLockAnnotation.key(),
                readLockAnnotation.timeout(),
                key -> lockContext.getRWLock(key).readLock(),
                readLockAnnotation);
    }

    // @WriteLock切面
    @Around("@annotation(writeLockAnnotation)")
    public Object writeLock(ProceedingJoinPoint pjp, WriteLock writeLockAnnotation) throws Throwable {
        return lock(
                pjp,
                writeLockAnnotation.condition(),
                writeLockAnnotation.key(),
                writeLockAnnotation.timeout(),
                key -> lockContext.getRWLock(key).writeLock(),
                writeLockAnnotation);
    }

    // 加锁
    private Object lock(ProceedingJoinPoint pjp,
                        String conditionExpression,
                        String keyExpression,
                        long timeout,
                        Function<String, Lock> lockFunction,
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
        // 计算加锁条件
        if (StringUtils.isNotEmpty(conditionExpression)) {
            boolean condition = evaluator.evalCondition(conditionExpression, evalContext, methodKey);
            if (!condition) {
                return pjp.proceed();
            }
        }
        // 计算key
        Object key = evaluator.evalKey(keyExpression, evalContext, methodKey);
        if (key == null) {
            throw new IllegalArgumentException(String.format("key不能为null（key表达式可能有错误）：method=%s,锁注解=%s", method, annotation));
        }
        // 获取锁
        Lock lock = lockFunction.apply(key.toString());
        if (timeout < 0) {
            // 无限等待加锁
            lock.lock();
        } else {
            // 指定超时时间加锁
            boolean success = lock.tryLock(timeout, TimeUnit.MILLISECONDS);
            if (!success) {
                throw new TimeoutException(String.format("等待加锁超时：method=%s,锁注解=%s,key=%s", method, annotation, key));
            }
        }
        try {
            return pjp.proceed();
        } finally {
            // 解锁
            lock.unlock();
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
