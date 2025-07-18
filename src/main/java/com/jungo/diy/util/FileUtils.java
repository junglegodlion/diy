package com.jungo.diy.util;

import java.io.File;
import java.io.IOException;

/**
 * @author lichuang3
 * @date 2025-07-18 16:57
 */
public class FileUtils {
    public static void ensureDirectoryExists(String path) throws IOException {
        File directory = new File(path);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("无法创建目录: " + path);
        }
    }
}
