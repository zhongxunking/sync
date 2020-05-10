/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-09 18:21 创建
 */
package org.antframework.sync.extension.local.support;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 本地读写锁服务端
 */
public class LocalRWLockServer {
    // 监听器管理器
    private final ListenerManager listenerManager = new ListenerManager();
    // 所有读写者
    private final Map<String, RWLock> rwLocks = new ConcurrentHashMap<>();

    /**
     * 加读锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     * @return null 加锁成功；否则返回需等待的时间（毫秒）
     */
    public Long lockForRead(String key, String lockerId) {
        Long waitTime = null;
        AtomicBoolean success = new AtomicBoolean(false);
        visitTemplate(key, rwLock -> success.set(rwLock.lockForRead(lockerId)));
        if (!success.get()) {
            waitTime = TimeUnit.HOURS.toMillis(1);
        }
        return waitTime;
    }

    /**
     * 解读锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     */
    public void unlockForRead(String key, String lockerId) {
        visitTemplate(key, rwLock -> rwLock.unlockForRead(lockerId));
    }

    /**
     * 加写锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     * @param deadline 截止时间
     * @return null 加锁成功；否则返回需等待的时间（毫秒）
     */
    public Long lockForWrite(String key, String lockerId, long deadline) {
        Long waitTime = null;
        AtomicBoolean success = new AtomicBoolean(false);
        visitTemplate(key, rwLock -> success.set(rwLock.lockForWrite(lockerId, deadline)));
        if (!success.get()) {
            waitTime = TimeUnit.HOURS.toMillis(1);
        }
        return waitTime;
    }

    /**
     * 解写锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     */
    public void unlockForWrite(String key, String lockerId) {
        visitTemplate(key, rwLock -> rwLock.unlockForWrite(lockerId));
    }

    // 访问模版方法
    private void visitTemplate(String key, Consumer<RWLock> visitor) {
        rwLocks.compute(key, (k, v) -> {
            if (v == null) {
                v = new RWLock(k);
            }
            visitor.accept(v);
            if (v.isEmpty()) {
                v = null;
            }
            return v;
        });
    }

    /**
     * 新增同步监听器
     *
     * @param key      锁标识
     * @param listener 监听器
     */
    public void addSyncListener(String key, Runnable listener) {
        listenerManager.add(key, listener);
    }

    /**
     * 删除同步监听器
     *
     * @param key      锁标识
     * @param listener 监听器
     */
    public void removeSyncListener(String key, Runnable listener) {
        listenerManager.remove(key, listener);
    }

    // 锁持有者
    enum LockOwner {
        // 无
        NONE,
        // 读者
        READERS,
        // 写者
        WRITER,
        // 读写者
        READER_WRITER
    }

    // 读写锁
    private class RWLock {
        // 锁标识
        private final String key;
        // 持有者
        private LockOwner owner = LockOwner.NONE;
        // 写者预定截止时间
        private Long writerBooking = null;
        // 写者
        private String writer = null;
        // 读者
        private final Set<String> readers = new HashSet<>();

        public RWLock(String key) {
            this.key = key;
        }

        // 加读锁
        boolean lockForRead(String lockerId) {
            if (owner == LockOwner.NONE || owner == LockOwner.READERS) {
                if (writerBooking == null || writerBooking < System.currentTimeMillis()) {
                    owner = LockOwner.READERS;
                    readers.add(lockerId);
                    writerBooking = null;
                }
            } else if (owner == LockOwner.WRITER) {
                if (Objects.equals(lockerId, writer)) {
                    owner = LockOwner.READER_WRITER;
                    readers.add(lockerId);
                }
            }
            return readers.contains(lockerId);
        }

        // 解读锁
        void unlockForRead(String lockerId) {
            if (owner == LockOwner.READERS || owner == LockOwner.READER_WRITER) {
                readers.remove(lockerId);
                if (readers.isEmpty()) {
                    if (owner == LockOwner.READERS) {
                        owner = LockOwner.NONE;
                    } else {
                        owner = LockOwner.WRITER;
                    }
                }
            }
            if (readers.size() <= 1) {
                listenerManager.publish(key);
            }
        }

        // 加写锁
        boolean lockForWrite(String lockerId, long deadline) {
            if (owner == LockOwner.NONE) {
                owner = LockOwner.WRITER;
                writer = lockerId;
                writerBooking = null;
            } else if (owner == LockOwner.READERS) {
                if (readers.size() == 1 && readers.contains(lockerId)) {
                    owner = LockOwner.READER_WRITER;
                    writer = lockerId;
                    writerBooking = null;
                } else {
                    writerBooking = deadline;
                }
            }
            return Objects.equals(writer, lockerId);
        }

        // 解写锁
        void unlockForWrite(String lockerId) {
            if (owner == LockOwner.WRITER || owner == LockOwner.READER_WRITER) {
                if (Objects.equals(lockerId, writer)) {
                    writer = null;
                    if (owner == LockOwner.WRITER) {
                        owner = LockOwner.NONE;
                    } else {
                        owner = LockOwner.READERS;
                    }
                }
            }
            listenerManager.publish(key);
        }

        // 是否为空
        boolean isEmpty() {
            return owner == LockOwner.NONE && (writerBooking == null || writerBooking < System.currentTimeMillis());
        }
    }
}
