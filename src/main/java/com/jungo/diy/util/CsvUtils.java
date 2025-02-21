package com.jungo.diy.util;


import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author lichuang3
 */
public class CsvUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final LocalDate DATE_THRESHOLD = LocalDate.of(2025, 1, 17);
    private static final String HOST_NAME = "cl-gateway.tuhu.cn";
    public static List<List<String>> getData(String str, String directoryName) {
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
                // 对collect进行处理
                List<String> newCollect = getNewCollect(collect, directoryName);
                data.add(newCollect);
            }
        }
        return data;
    }

    /**
     * 根据目录名日期判断是否需要修改原始集合，若目录日期早于阈值则在头部添加主机名
     *
     * @param collect        原始字符串集合，可能被修改的基础集合
     * @param directoryName  目录名称（需符合DATE_FORMATTER格式的日期字符串）
     * @return 若满足日期条件则返回添加了HOST_NAME的新集合，否则返回原始集合
     *         返回的集合顺序：当添加时HOST_NAME位于集合第一个元素位置
     */
    private static List<String> getNewCollect(List<String> collect, String directoryName) {
        LocalDate localDate = LocalDate.parse(directoryName, DATE_FORMATTER);

        if (localDate.isBefore(DATE_THRESHOLD)) {
            List<String> newCollect = new ArrayList<>(collect.size() + 1);
            newCollect.add(HOST_NAME);
            newCollect.addAll(collect);
            return newCollect;
        }
        return collect;
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
            if (containsOtherSpecialChars(str)) {
                return false;
            }
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

    // 判断字符串是否包含除"-"、"="和"/"的特殊字符
    public static boolean containsOtherSpecialChars(String input) {
        // 定义正则表达式，匹配除字母、数字、'-'、'=' 和 '/' 之外的字符
        // 定义正则表达式，使用字符类的否定形式匹配目标字符
        String regex = "[^a-zA-Z0-9\\-=./]";
        return Pattern.compile(regex).matcher(input).find();
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