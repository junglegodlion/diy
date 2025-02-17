package com.jungo.diy.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileReaderService {
    // 指定本地路径（Windows格式）
    private static final String PREFIX = "C:\\Users\\Lichuang3\\Desktop\\备份\\c端网关接口性能统计\\数据收集\\";

    @Value("${file.storage.dir}")
    private String targetDir;
    public List<String> readTargetFiles() {
        // 校验路径是否为空或包含非法字符
        if (targetDir == null || targetDir.trim().isEmpty()) {
            throw new IllegalArgumentException("目标目录不能为空");
        }

        // 尝试解析路径
        Path dirPath;
        try {
            dirPath = Paths.get(PREFIX + targetDir);
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("路径包含非法字符: " + e.getMessage());
        }

        // 路径校验
        if (!Files.exists(dirPath)  || !Files.isDirectory(dirPath))  {
            throw new IllegalArgumentException("目录不存在或不是文件夹");
        }

        try (Stream<Path> paths = Files.list(dirPath))  {
            return paths.filter(Files::isRegularFile)
                    .limit(4)
                    .map(path -> {
                        try {
                            return new String(Files.readAllBytes(path),  StandardCharsets.UTF_8);
                        } catch (IOException e) {
                            return "读取失败：" + path.getFileName();
                        }
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("目录访问失败", e);
        }
    }
}