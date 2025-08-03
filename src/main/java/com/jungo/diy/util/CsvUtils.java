package com.jungo.diy.util;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author lichuang3
 */
@Slf4j
public class CsvUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final LocalDate DATE_THRESHOLD = LocalDate.of(2025, 1, 17);
    private static final String HOST_NAME = "cl-gateway.tuhu.cn";

    public static List<List<String>> getLists(MultipartFile file) {
        return getLists(file, false);
    }

    public static List<List<String>> getLists(MultipartFile file, boolean skipFirstRow) {
        List<List<String>> csvData = null;
        try {
            csvData = CsvUtils.getDataFromInputStream(file.getInputStream(), skipFirstRow);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return csvData;
    }

    /**
     * 从指定路径读取CSV数据
     * @param directoryName 目标目录名 (可优化为更具体的参数名)
     * @param path 文件路径
     * @return 非null的CSV数据列表，出错时返回空列表
     */
    public static List<List<String>> readCsvData(String directoryName, Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            return getDataFromInputStream(directoryName, is);
        } catch (IOException e) {
            log.error("Failed to read CSV from {}: {}", directoryName, path, e);
        }
        return Collections.emptyList();
    }


    /**
     * 从输入流解析CSV数据并进行必要转换
     * @param directoryName 用于数据转换的目录标识
     * @param inputStream 输入流(自动关闭)
     * @return 处理后的CSV数据列表，不会返回null
     */
    public static List<List<String>> getDataFromInputStream(String directoryName, InputStream inputStream) {
        List<List<String>> newData = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            CSVParser parser = CSVFormat.DEFAULT.parse(reader);
            for (CSVRecord record : parser) {
                // 将CSV记录转换为字符串列表
                List<String> row = StreamSupport.stream(record.spliterator(), false)
                        .collect(Collectors.toList());
                // 检查当前行是否满足条件（包含斜杠）
                if (checkListForSlash(row)) {
                    // 如果满足条件，则处理该行并添加到结果中
                    newData.add(getNewCollect(row, directoryName));
                }
            }
        } catch (IOException e) {
            log.error("CsvUtils#getData,出现异常！", e);
        }
        return newData;
    }

    public static List<List<String>> getDataFromInputStream(InputStream inputStream, boolean skipFirstRow) {
        List<List<String>> data = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            CSVParser parser = CSVFormat.DEFAULT.parse(reader);
            boolean isFirstRow = true;
            for (CSVRecord record : parser) {
                // 根据参数决定是否跳过第一行
                if (skipFirstRow && isFirstRow) {
                    isFirstRow = false;
                    continue;
                }
                List<String> row = new ArrayList<>();
                record.forEach(row::add);
                data.add(row);
            }
        } catch (IOException e) {
            log.error("CsvUtils#getDataFromInputStream,出现异常！", e);
        }
        return data;
    }

    // 重载方法，保持向后兼容，默认不跳过第一行
    public static List<List<String>> getDataFromInputStream(InputStream inputStream) {
        return getDataFromInputStream(inputStream, false);
    }


    /**
     * 根据目录名称日期判断是否在阈值日期之前，决定是否在集合头部添加主机名
     * @param collect 原始字符串集合，不能为null
     * @param directoryName 目录名称字符串，应包含符合DATE_FORMATTER格式的日期，不能为null
     * @return 处理后的新集合：
     *         - 如果directoryName日期解析失败，返回原始集合
     *         - 如果目录日期早于阈值日期DATE_THRESHOLD，返回添加了HOST_NAME的新集合
     *         - 否则返回原始集合
     * @throws NullPointerException 如果collect或directoryName为null
     */
    private static List<String> getNewCollect(List<String> collect, String directoryName) {
        Objects.requireNonNull(collect, "collect must not be null");
        Objects.requireNonNull(directoryName, "directoryName must not be null");
        LocalDate localDate;
        try {
            localDate = LocalDate.parse(directoryName, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("Invalid date format for directoryName: {}", directoryName, e);
            return collect;
        }

        // 判断解析出的日期是否早于阈值日期
        if (localDate.isBefore(DATE_THRESHOLD)) {
            // 在集合开头添加HOST_NAME并返回新集合
            return Stream.concat(Stream.of(HOST_NAME), collect.stream())
                    .collect(Collectors.toList());
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