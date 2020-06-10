/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-06-09 22:16 创建
 */
package org.antframework.sync.lock;

import lombok.AllArgsConstructor;
import org.antframework.sync.AbstractTest;
import org.antframework.sync.Performance;
import org.antframework.sync.SyncContext;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Consumer;

/**
 * 读写锁单元测试
 */
public class RWLockTest extends AbstractTest {
    // 锁的标识
    private static final String KEY = "123";
    // 循环次数
    private static final int LOOP = 1000;

    private int left = 0;
    private int right = 0;

    @Test
    public void test() throws InterruptedException {
        System.out.println("=====================RWLock-start=======================");
        CountDownLatch latch = new CountDownLatch(AMOUNT_OF_THREAD);

        int amountOfReader = AMOUNT_OF_THREAD * 2 / 3;
        Performance[] readerPerformances = new Performance[amountOfReader];
        for (int i = 0; i < amountOfReader; i++) {
            int index = i;
            executor.execute(new ReaderTask(syncContext, KEY, LOOP, performance -> {
                readerPerformances[index] = performance;
                latch.countDown();
            }));
        }

        int amountOfWriter = AMOUNT_OF_THREAD - amountOfReader;
        Performance[] writerPerformances = new Performance[amountOfWriter];
        for (int i = 0; i < amountOfWriter; i++) {
            int index = i;
            executor.execute(new WriterTask(syncContext, KEY, LOOP, performance -> {
                writerPerformances[index] = performance;
                latch.countDown();
            }));
        }

        latch.await();

        double tpsOfReader = 0;
        for (int i = 0; i < amountOfReader; i++) {
            Performance performance = readerPerformances[i];
            System.out.println("读者任务" + i + ":" + performance.toString());
            performance.check();
            tpsOfReader += performance.getTps();
        }
        System.out.println(String.format("读者总tps=%.2f", tpsOfReader));

        double tpsOfWriter = 0;
        for (int i = 0; i < amountOfWriter; i++) {
            Performance performance = writerPerformances[i];
            System.out.println("写者任务" + i + ":" + performance.toString());
            performance.check();
            tpsOfWriter += performance.getTps();
        }
        System.out.println(String.format("写者总tps=%.2f", tpsOfWriter));

        System.out.println("left=" + left + ",right=" + right);
        Assert.assertEquals(left, right);
        Assert.assertEquals(left, LOOP * amountOfWriter);
        System.out.println("=====================RWLock-end=======================");
    }

    // 读者任务
    @AllArgsConstructor
    private class ReaderTask implements Runnable {
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
                    ReadWriteLock rwLock = syncContext.getLockContext().getRWLock(key);
                    rwLock.readLock().lock();
                    try {
                        int leftTemp = left;
//                        Thread.sleep(1);
                        int rightTemp = right;
                        if (leftTemp != rightTemp) {
                            throw new IllegalStateException("读到脏数据：left=" + leftTemp + ",right=" + rightTemp);
                        }
                    } finally {
                        rwLock.readLock().unlock();
                    }
                }
            } catch (Throwable e) {
                System.out.println("RWLockTest出错:" + e);
                success = false;
            } finally {
                long endTime = System.currentTimeMillis();
                double tps = loop * 1000D / (endTime - startTime);
                consumer.accept(new Performance(startTime, endTime, loop, tps, success));
            }
        }
    }

    // 写者任务
    @AllArgsConstructor
    private class WriterTask implements Runnable {
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
                    ReadWriteLock rwLock = syncContext.getLockContext().getRWLock(key);
                    rwLock.writeLock().lock();
                    try {
                        left++;
//                        Thread.sleep(1);
                        right++;
                    } finally {
                        rwLock.writeLock().unlock();
                    }
                }
            } catch (Throwable e) {
                System.out.println("RWLockTest出错:" + e);
                success = false;
            } finally {
                long endTime = System.currentTimeMillis();
                double tps = loop * 1000D / (endTime - startTime);
                consumer.accept(new Performance(startTime, endTime, loop, tps, success));
            }
        }
    }
}
