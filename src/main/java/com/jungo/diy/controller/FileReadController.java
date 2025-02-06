package com.jungo.diy.controller;

import com.jungo.diy.model.ExcelModel;
import com.jungo.diy.model.InterfacePerformanceModel;
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
    public List<InterfacePerformanceModel> readFile(@RequestParam("file") MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (Objects.isNull(filename)) {
            throw new IllegalArgumentException("文件名为空");
        }

        ExcelModel data = null;
        if (filename.endsWith(".xlsx")) {
            data = readXlsxFile(file);
        } else {
            throw new IllegalArgumentException("不支持的文件类型");
        }

        List<InterfacePerformanceModel> interfacePerformanceModels = new ArrayList<>();
        for (List<String> datum : data.getSheetModels().get(0).getData()) {
            InterfacePerformanceModel interfacePerformanceModel = new InterfacePerformanceModel();
            interfacePerformanceModel.setUrl(datum.get(0));
            interfacePerformanceModel.setTotalRequestCount(datum.get(1));
            interfacePerformanceModel.setP99(datum.get(3));
            interfacePerformanceModels.add(interfacePerformanceModel);
        }
        return interfacePerformanceModels;
    }

    private ExcelModel readXlsxFile(MultipartFile file) throws IOException {
        // XLSX 文件读取逻辑
        return fileService.readXlsxFile(file);
    }

    private List<List<String>> readCsvFile(MultipartFile file) throws IOException {
        // CSV 文件读取逻辑
        return fileService.readCsvFile(file);
    }
}
