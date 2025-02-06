package com.jungo.diy.service;

import com.jungo.diy.model.ExcelModel;
import com.jungo.diy.model.SheetModel;
import com.jungo.diy.util.CsvUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lichuang3
 * @date 2025-02-06 12:55
 */
@Service
@Slf4j
public class FileService {

    public ExcelModel readXlsxFile(MultipartFile file) {

        ExcelModel excelModel = new ExcelModel();
        // 通过文件流创建工作簿
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            List<SheetModel> sheetModels = new ArrayList<>();
            int numberOfSheets = workbook.getNumberOfSheets();
            for (int i = 0; i < numberOfSheets; i++) {
                SheetModel sheetModel = new SheetModel();
                Sheet sheet = workbook.getSheetAt(i);
                sheetModel.setSheetIndex(i);
                sheetModel.setSheetName(sheet.getSheetName());
                List<List<String>> data = new ArrayList<>();
                for (Row row : sheet) {
                    List<String> rowData = new ArrayList<>();
                    // 读取单元格内容
                    for (Cell cell : row) {
                        rowData.add(getCellValueAsString(cell));
                    }
                    data.add(rowData);
                }
                sheetModel.setData(data);
                sheetModels.add(sheetModel);
            }
            excelModel.setSheetModels(sheetModels);
        } catch (Exception e) {
            log.error("FileService#readXlsxFile,出现异常！", e);
        }

        return excelModel;
    }

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

    public List<List<String>> readCsvFile(MultipartFile file) {
        try {
            return CsvUtils.parseCsvWithoutHeader(file);
        } catch (IOException e) {
            throw new RuntimeException("读取 CSV 文件时发生错误", e);
        }
    }
}
