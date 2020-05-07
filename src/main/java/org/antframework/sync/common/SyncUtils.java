/* 
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-03 16:50 创建
 */
package org.antframework.sync.common;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Sync工具类
 */
public class SyncUtils {
    /**
     * 获取脚本
     */
    public static String getScript(String path) {
        InputStream input = SyncUtils.class.getClassLoader().getResourceAsStream(path);
        if (input == null) {
            throw new IllegalArgumentException(String.format("资源[%s]不存在", path));
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, Charset.forName("utf-8")))) {
            String line = reader.readLine();
            while (line != null) {
                builder.append(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            ExceptionUtils.rethrow(e);
        }
        return builder.toString();
    }

    /**
     * 获取新id
     */
    public static String newId() {
        String id = UUID.randomUUID().toString();
        id = id.replace("-", "");
        return id;
    }
}
