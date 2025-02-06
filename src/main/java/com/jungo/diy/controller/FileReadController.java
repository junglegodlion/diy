package com.jungo.diy.controller;

import com.jungo.diy.model.ExcelModel;
import com.jungo.diy.model.InterfacePerformanceModel;
import com.jungo.diy.model.SheetModel;
import com.jungo.diy.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

        List<InterfacePerformanceModel> interfacePerformanceModels = getInterfacePerformanceModels(data.getSheetModels().get(0), data.getSheetModels().get(1));
        return interfacePerformanceModels;
    }

    private List<InterfacePerformanceModel> getInterfacePerformanceModels(SheetModel requestsheetModel, SheetModel slowRequestCountSheetModel) {
        // 将slowRequestCountSheetModel转换成map
        Map<String, Integer> slowRequestCountMap = slowRequestCountSheetModel.getData().stream().collect(Collectors.toMap(x -> x.get(0) + x.get(1), x -> convertStringToInteger(x.get(2)), (x, y) -> x));

        // 请求情况
        List<InterfacePerformanceModel> interfacePerformanceModels = new ArrayList<>();
        for (List<String> datum : requestsheetModel.getData()) {
            InterfacePerformanceModel interfacePerformanceModel = new InterfacePerformanceModel();
            String url = datum.get(0) + datum.get(1);
            interfacePerformanceModel.setUrl(datum.get(0) + datum.get(1));
            interfacePerformanceModel.setTotalRequestCount(convertStringToInteger(datum.get(2)));
            interfacePerformanceModel.setP99(convertStringToInteger(datum.get(4)));
            Integer slowRequestCount = slowRequestCountMap.get(url);
            if (Objects.isNull(slowRequestCount)) {
                slowRequestCount = 0;
            }
            interfacePerformanceModel.setSlowRequestCount(slowRequestCount);
            interfacePerformanceModels.add(interfacePerformanceModel);
        }

        return interfacePerformanceModels;
    }

    public static Integer convertStringToInteger(String input) {
        input = input.trim();
        try {
            BigDecimal bd = new BigDecimal(input);
            bd = bd.stripTrailingZeros(); // 去除末尾零（如 "9.000" → "9"）
            if (bd.scale() <= 0) { // 若小数位 ≤ 0，说明是整数
                return bd.intValueExact();
            } else {
                throw new NumberFormatException("输入包含非整数部分: " + input);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的数字格式: " + input, e);
        }
    }

    // 调用示例
    Integer result = convertStringToInteger("9.0"); // 返回 9

    private ExcelModel readXlsxFile(MultipartFile file) throws IOException {
        // XLSX 文件读取逻辑
        return fileService.readXlsxFile(file);
    }

    private List<List<String>> readCsvFile(MultipartFile file) throws IOException {
        // CSV 文件读取逻辑
        return fileService.readCsvFile(file);
    }
}
