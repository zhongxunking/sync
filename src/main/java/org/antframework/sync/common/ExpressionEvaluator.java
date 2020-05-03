/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-03 18:49 创建
 */
package org.antframework.sync.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表达式计算器
 */
public class ExpressionEvaluator extends CachedExpressionEvaluator {
    // 目标方法缓存
    private final Map<AnnotatedElementKey, Method> targetMethodCache = new ConcurrentHashMap<AnnotatedElementKey, Method>(64);
    // 条件表达式缓存
    private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<>(64);
    // key表达式缓存
    private final Map<ExpressionKey, Expression> keyCache = new ConcurrentHashMap<>(64);

    /**
     * 获取计算上下文
     *
     * @param targetClass 目标类
     * @param target      目标对象
     * @param method      方法
     * @param args        方法参数
     * @return 计算上下文
     */
    public EvaluationContext getEvalContext(Class<?> targetClass, Object target, Method method, Object[] args) {
        Object[] extractedArgs = extractArgs(method, args);
        ExpressionRoot root = new ExpressionRoot(targetClass, target, method, extractedArgs);
        Method targetMethod = getTargetMethod(targetClass, method);
        return new MethodBasedEvaluationContext(root, targetMethod, extractedArgs, getParameterNameDiscoverer());
    }

    /**
     * 提取参数（如果有可变参数，则打平可变参数）
     *
     * @param method 方法
     * @param args   方法参数
     * @return 提取出参数
     */
    private Object[] extractArgs(Method method, Object[] args) {
        if (!method.isVarArgs()) {
            return args;
        }
        Object[] varArgs = ObjectUtils.toObjectArray(args[args.length - 1]);
        Object[] combinedArgs = new Object[args.length - 1 + varArgs.length];
        System.arraycopy(args, 0, combinedArgs, 0, args.length - 1);
        System.arraycopy(varArgs, 0, combinedArgs, args.length - 1, varArgs.length);
        return combinedArgs;
    }

    // 获取目标方法
    private Method getTargetMethod(Class<?> targetClass, Method method) {
        AnnotatedElementKey methodKey = new AnnotatedElementKey(method, targetClass);
        return targetMethodCache.computeIfAbsent(methodKey, k -> AopUtils.getMostSpecificMethod(method, targetClass));
    }

    /**
     * 计算条件
     *
     * @param expression  表达式
     * @param evalContext 计算上下文
     * @param methodKey   方法key
     * @return true：通过；false：驳回
     */
    public boolean evalCondition(String expression, EvaluationContext evalContext, AnnotatedElementKey methodKey) {
        return getExpression(conditionCache, methodKey, expression).getValue(evalContext, boolean.class);
    }

    /**
     * 计算key
     *
     * @param expression  表达式
     * @param evalContext 计算上下文
     * @param methodKey   方法key
     * @return key
     */
    public Object evalKey(String expression, EvaluationContext evalContext, AnnotatedElementKey methodKey) {
        return getExpression(keyCache, methodKey, expression).getValue(evalContext);
    }

    /**
     * 表达式根节点
     */
    @AllArgsConstructor
    @Getter
    private static class ExpressionRoot {
        // 目标类
        private final Class<?> targetClass;
        // 目标对象
        private final Object target;
        // 方法
        private final Method method;
        // 方法参数
        private final Object[] args;
    }
}
