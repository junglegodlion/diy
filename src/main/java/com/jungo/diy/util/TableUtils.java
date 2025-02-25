package com.jungo.diy.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;

import java.math.BigDecimal;

/**
 * @author lichuang3
 * @date 2025-02-25 10:17
 */
public class TableUtils {
    // 设置单元格为百分比格式
    public static void setCellPercentageFormat(Cell cell) {
        CellStyle cellStyle = cell.getCellStyle();
        DataFormat dataFormat = cell.getSheet().getWorkbook().createDataFormat();
        short percentageFormat = dataFormat.getFormat("0.00%");
        cellStyle.setDataFormat(percentageFormat);
        cell.setCellStyle(cellStyle);
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
}
