package com.jungo.diy.controller;

import com.jungo.diy.service.FileService;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author lichuang3
 * @date 2025-02-06 12:51
 */
@RestController
public class FileReadController {

    @Autowired
    FileService fileService;

    @PostMapping("/upload")
    public List<List<String>> readFile(@RequestParam("file") MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (Objects.isNull(filename)) {
            throw new IllegalArgumentException("文件名为空");
        }

        if (filename.endsWith(".xlsx")) {
            return readXlsxFile(file);
        } else if (filename.endsWith(".csv")) {
            return readCsvFile(file);
        } else {
            throw new IllegalArgumentException("不支持的文件类型");
        }
    }

    private List<List<String>> readXlsxFile(MultipartFile file) throws IOException {
        // XLSX 文件读取逻辑
        return fileService.readXlsxFile(file);
    }

    private List<List<String>> readCsvFile(MultipartFile file) throws IOException {
        // CSV 文件读取逻辑
        return fileService.readCsvFile(file);
    }
}
