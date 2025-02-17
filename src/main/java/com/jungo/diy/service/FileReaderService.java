package com.jungo.diy.service;


import com.jungo.diy.model.PerformanceFileModel;
import com.jungo.diy.model.PerformanceFolderModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class FileReaderService {
    // 指定本地路径（Windows格式）
    private static final String PREFIX = "C:\\Users\\Lichuang3\\Desktop\\备份\\c端网关接口性能统计\\数据收集\\";

    @Value("${file.storage.dir}")
    private String targetDir;

    public String readTargetFiles() {
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
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            throw new IllegalArgumentException("目录不存在或不是文件夹");
        }
        PerformanceFolderModel performanceFolderModel = new PerformanceFolderModel();
        performanceFolderModel.setFolderName(targetDir);
        List<PerformanceFileModel> files = new ArrayList<>();
        performanceFolderModel.setFiles(files);

        try (Stream<Path> paths = Files.list(dirPath)) {
            paths.filter(Files::isRegularFile)
                    .limit(4)
                    .forEach(path -> {
                        try {
                            String fileName = path.getFileName().toString();
                            PerformanceFileModel performanceFileModel = new PerformanceFileModel();
                            performanceFileModel.setFileName(fileName);
                            String str = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                            String[] lines = str.split("\\R");
                            List<List<String>> data = new ArrayList<>();
                            performanceFileModel.setData(data);
                            for (String line : lines) {
                                // 去除首尾空格后按逗号分割（支持逗号前后有空格）
                                String[] parts = line.trim().split("\\s*,\\s*");
                                List<String> collect = Arrays.stream(parts).map(String::trim).collect(Collectors.toList());
                                data.add(collect);
                            }
                            files.add(performanceFileModel);

                        } catch (IOException e) {
                            log.error("FileReaderService#readTargetFiles,出现异常！", e);
                        }
                    });

        } catch (IOException e) {
            throw new RuntimeException("目录访问失败", e);
        }
        return "success";
    }
}