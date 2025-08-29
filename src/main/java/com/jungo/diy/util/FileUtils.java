package com.jungo.diy.util;

import com.jungo.diy.constants.FileConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @author lichuang3
 * @date 2025-07-18 16:57
 */
@Slf4j
public class FileUtils {
    public static void ensureDirectoryExists(String path) throws IOException {
        File directory = new File(path);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("无法创建目录: " + path);
        }
    }

    public static String buildOutputFileName(String fileName) throws UnsupportedEncodingException {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());
    }

    public static void saveWorkbookToFile(XSSFWorkbook workbook, String directoryPath, String fileName) throws IOException {
        // 确保目录存在，如果不存在则创建
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                log.warn("无法创建目录: {}", directoryPath);
            }
        }

        File file = new File(directory, fileName);
        try (FileOutputStream out = new FileOutputStream(file)) {
            workbook.write(out);
        }
    }

    public static String buildOutputDirectory(String dateStr) {
        return FileConstants.OUTPUT_DIRECTORY + "/" + dateStr;
    }
}
