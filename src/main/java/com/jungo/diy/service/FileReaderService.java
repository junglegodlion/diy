package com.jungo.diy.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileReaderService {
    // 指定本地路径（Windows格式）
    private static final String TARGET_DIR = "C:\\Users\\Lichuang3\\Desktop\\备份\\c端网关接口性能统计\\数据收集\\2025-02-13";
 
    public List<String> readTargetFiles() {
        List<String> fileContents = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(TARGET_DIR)))  {
            int count = 0;
            for (Path path : stream) {
                if (count >= 4) break; // 限制读取4个文件 
                if (Files.isRegularFile(path))  {
                    List<String> lines = Files.readAllLines(path);
                    String content = String.join("\n", lines);
                    fileContents.add(" 文件名：" + path.getFileName()  + "\n内容：" + content);
                    count++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("文件读取失败：" + e.getMessage()); 
        }
        return fileContents;
    }
}