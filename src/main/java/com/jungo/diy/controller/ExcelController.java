package com.jungo.diy.controller;

import org.apache.poi.ss.usermodel.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lichuang3
 */
@RestController
public class ExcelController {

    @PostMapping("/upload")
    public List<List<String>> readExcel(@RequestParam("file") MultipartFile file) throws IOException {
        List<List<String>> data = new ArrayList<>();

        // 通过文件流创建工作簿
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            // 读取第一个 Sheet
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                List<String> rowData = new ArrayList<>();
                // 读取单元格内容
                for (Cell cell : row) {

                    rowData.add(getCellValueAsString(cell));
                }
                data.add(rowData);
            }
        }

        return data;
    }

    // 处理不同格式的单元格数据
    private String getCellValueAsString(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}