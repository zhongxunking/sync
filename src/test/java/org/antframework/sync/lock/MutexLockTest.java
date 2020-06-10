/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-06-09 21:32 创建
 */
package org.antframework.sync.lock;

import lombok.AllArgsConstructor;
import org.antframework.sync.AbstractTest;
import org.antframework.sync.Performance;
import org.antframework.sync.SyncContext;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;

/**
 * 互斥锁单元测试
 */
public class MutexLockTest extends AbstractTest {
    // 锁的标识
    private static final String KEY = "123";
    // 循环次数
    private static final int LOOP = 1000;

    private int amount = 0;

    // 测试
    @Test
    public void test() throws InterruptedException {
        System.out.println("=====================MutexLock-start=======================");
        Performance[] performances = new Performance[AMOUNT_OF_THREAD];
        CountDownLatch latch = new CountDownLatch(AMOUNT_OF_THREAD);
        for (int i = 0; i < AMOUNT_OF_THREAD; i++) {
            int index = i;
            executor.execute(new Task(syncContext, KEY, LOOP, performance -> {
                performances[index] = performance;
                latch.countDown();
            }));
        }
        latch.await();
        double tps = 0;
        for (int i = 0; i < AMOUNT_OF_THREAD; i++) {
            Performance performance = performances[i];
            System.out.println("任务" + i + ":" + performance.toString());
            performance.check();
            tps += performance.getTps();
        }
        System.out.println(String.format("总tps=%.2f", tps));
        System.out.println("amount=" + amount);
        Assert.assertEquals(LOOP * AMOUNT_OF_THREAD, amount);
        System.out.println("=====================MutexLock-end=======================");
    }

    // 任务
    @AllArgsConstructor
    private class Task implements Runnable {
        // Sync上下文
        private final SyncContext syncContext;
        // 锁的标识
        private final String key;
        // 循环次数
        private final int loop;
        // 运行结果消费者
        private final Consumer<Performance> consumer;

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            boolean success = true;
            try {
                for (int i = 0; i < loop; i++) {
                    Lock lock = syncContext.getLockContext().getLock(key);
                    lock.lock();
                    try {
                        int temp = amount;
//                        Thread.sleep(1);
                        amount = temp + 1;
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (Throwable e) {
                System.out.println("MutexLockTest出错:" + e);
                success = false;
            } finally {
                long endTime = System.currentTimeMillis();
                double tps = loop * 1000D / (endTime - startTime);
                consumer.accept(new Performance(startTime, endTime, loop, tps, success));
            }
        }
    }
}
