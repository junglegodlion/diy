package com.jungo.diy.util;


import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lichuang3
 */
public class CsvUtils {

    /**
     * 读取 CSV 为 List<Map>（首行为表头）
     */
    public static List<Map<String, String>> parseCsvWithHeader(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            CSVParser parser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader() // 首行为表头
                    .parse(reader);

            return parser.getRecords().stream()
                    .map(CSVRecord::toMap) // 转为 Map<字段名, 值>
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