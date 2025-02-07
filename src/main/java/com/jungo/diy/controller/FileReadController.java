package com.jungo.diy.controller;

import com.jungo.diy.model.ExcelModel;
import com.jungo.diy.model.InterfacePerformanceModel;
import com.jungo.diy.model.SheetModel;
import com.jungo.diy.model.UrlPerformanceModel;
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
    public List<UrlPerformanceModel> readFile(@RequestParam("file") MultipartFile file) throws IOException {
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

        // 上周的接口性能数据
        List<InterfacePerformanceModel> lastWeek = getInterfacePerformanceModels(data.getSheetModels().get(0), data.getSheetModels().get(1));
        // 本周的接口性能数据
        List<InterfacePerformanceModel> thisWeek = getInterfacePerformanceModels(data.getSheetModels().get(2), data.getSheetModels().get(3));
        // 将lastWeek转换成map
        Map<String, InterfacePerformanceModel> lastWeekMap = lastWeek.stream().collect(Collectors.toMap(InterfacePerformanceModel::getToken, x -> x, (x, y) -> x));
        // 将thisWeek转换成map
        Map<String, InterfacePerformanceModel> thisWeekMap = thisWeek.stream().collect(Collectors.toMap(InterfacePerformanceModel::getToken, x -> x, (x, y) -> x));
        // 组装UrlPerformanceModel对象
        List<UrlPerformanceModel> urlPerformanceModels = new ArrayList<>();
        for (Map.Entry<String, InterfacePerformanceModel> entry : thisWeekMap.entrySet()) {
            String token = entry.getKey();
            InterfacePerformanceModel thisWeekInterfacePerformanceModel = entry.getValue();
            InterfacePerformanceModel lastWeekInterfacePerformanceModel = lastWeekMap.get(token);
            if (Objects.nonNull(lastWeekInterfacePerformanceModel)) {
                UrlPerformanceModel urlPerformanceModel = new UrlPerformanceModel();
                urlPerformanceModel.setToken(token);
                urlPerformanceModel.setHost(thisWeekInterfacePerformanceModel.getHost());
                urlPerformanceModel.setUrl(thisWeekInterfacePerformanceModel.getUrl());
                urlPerformanceModel.setLastWeek(lastWeekInterfacePerformanceModel);
                urlPerformanceModel.setThisWeek(thisWeekInterfacePerformanceModel);
                urlPerformanceModels.add(urlPerformanceModel);
            }

        }
        return urlPerformanceModels;
    }

    private List<InterfacePerformanceModel> getInterfacePerformanceModels(SheetModel requestsheetModel, SheetModel slowRequestCountSheetModel) {
        // 将slowRequestCountSheetModel转换成map
        Map<String, Integer> slowRequestCountMap = slowRequestCountSheetModel.getData().stream().collect(Collectors.toMap(x -> x.get(0) + x.get(1), x -> convertStringToInteger(x.get(2)), (x, y) -> x));

        // 请求情况
        List<InterfacePerformanceModel> interfacePerformanceModels = new ArrayList<>();
        for (List<String> datum : requestsheetModel.getData()) {
            InterfacePerformanceModel interfacePerformanceModel = new InterfacePerformanceModel();
            String token = datum.get(0) + datum.get(1);
            interfacePerformanceModel.setToken(token);
            interfacePerformanceModel.setHost(datum.get(0));
            interfacePerformanceModel.setUrl(datum.get(1));
            interfacePerformanceModel.setTotalRequestCount(convertStringToInteger(datum.get(2)));
            interfacePerformanceModel.setP99(convertStringToInteger(datum.get(4)));
            Integer slowRequestCount = slowRequestCountMap.get(token);
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
            // 去除末尾零（如 "9.000" → "9"）
            bd = bd.stripTrailingZeros();
            // 若小数位 ≤ 0，说明是整数
            if (bd.scale() <= 0) {
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
