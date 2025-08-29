package com.jungo.diy.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class ApiUrlReader {

    /**
     * 从 resources 下的 api-urls.txt 文件中读取所有 API URL
     *
     * @param fileName 文件名（例如 "api-urls.txt"）
     * @return url列表
     */
    public static List<String> readApiUrls(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource(fileName);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines()
                        .map(String::trim) // 去掉前后空格
                        .filter(line -> !line.isEmpty()) // 过滤空行
                        .map(line -> line.replaceAll("[\",]", "")) // 去掉文件中可能的引号和逗号
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            throw new RuntimeException("读取文件失败: " + fileName, e);
        }
    }

    public static void main(String[] args) {
        List<String> apiUrls = readApiUrls("api-urls.txt");
        apiUrls.forEach(System.out::println);
    }
}
