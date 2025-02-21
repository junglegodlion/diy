package com.jungo.diy.util;


import com.jungo.diy.model.InterfacePerformanceModel;
import com.jungo.diy.service.FileReaderService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lichuang3
 */
public class CsvUtils {

    public static List<List<String>> getData(String str) {
        if (StringUtils.isBlank(str)) {
            return Collections.emptyList();
        }
        // \\R表示换行符，用于匹配任意换行符
        String[] lines = str.split("\\R");
        List<List<String>> data = new ArrayList<>();
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (StringUtils.isBlank(trimmedLine)) {
                continue;
            }
            // 去除首尾空格后按逗号分割（支持逗号前后有空格）
            String[] parts = line.trim().split("\\s*,\\s*");
            List<String> collect = Arrays.stream(parts).map(s -> cleanSpecialQuotes(s.trim())).collect(Collectors.toList());
            if (checkListForSlash(collect)) {
                data.add(collect);
            }
        }
        return data;
    }

    /**
     * 检查字符串列表中是否最多只有一个字符串包含 "/"或"="
     * 此方法用于确保列表中包含"/"或"="的字符串数量不超过一个
     *
     * @param list 待检查的字符串列表
     * @return 如果列表中包含"/"的字符串不超过一个，则返回true；否则返回false
     */
    public static boolean checkListForSlash(List<String> list) {
        int count = 0;

        // 遍历list，统计包含 "/" 的字符串个数
        for (String str : list) {
            if (str.contains("/") || str.contains("=")) {
                count++;
            }
            if (count > 1) {
                // 如果有2个及以上的字符串包含 "/"，返回false
                return false;
            }
        }

        // 如果列表中包含"/"的字符串不超过一个，返回true
        return true;
    }

    public static String cleanSpecialQuotes(String input) {
        return StringUtils.strip(input, "\"");
    }
    /**
     * 读取 CSV 为 List<Map>（首行为表头）
     */
    public static List<Map<String, String>> parseCsvWithHeader(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            CSVParser parser = CSVFormat.DEFAULT
                    // 首行为表头
                    .withFirstRecordAsHeader()
                    .parse(reader);

            return parser.getRecords().stream()
                    // 转为 Map<字段名, 值>
                    .map(CSVRecord::toMap)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 读取 CSV 为 List<List>（无表头）
     */
    public static List<List<String>> parseCsvWithoutHeader(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            CSVParser parser = CSVFormat.DEFAULT.parse(reader);

            List<List<String>> data = new ArrayList<>();
            for (CSVRecord record : parser) {
                List<String> row = new ArrayList<>();
                record.forEach(row::add);
                data.add(row);
            }
            return data;
        }
    }
}