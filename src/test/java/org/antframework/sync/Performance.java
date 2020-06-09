/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-04-01 19:15 创建
 */
package org.antframework.sync;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.Assert;

/**
 * 性能
 */
@AllArgsConstructor
@Getter
public class Performance {
    // 开始时间
    private final long startTime;
    // 结束时间
    private final long endTime;
    // 循环次数
    private final int loop;
    // tps
    private final double tps;
    // 是否成功
    private boolean success;

    public void check() {
        Assert.assertTrue(success);
    }

    @Override
    public String toString() {
        return String.format("开始时间=%s，结束时间=%s，耗时=%d毫秒，循环次数=%d，tps=%.2f，是否成功=%s",
                DateFormatUtils.format(startTime, "yyyy-MM-dd HH:mm:ss.SSS"),
                DateFormatUtils.format(endTime, "yyyy-MM-dd HH:mm:ss.SSS"),
                endTime - startTime,
                loop,
                tps,
                success);
    }
}
