package com.jungo.diy.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;

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
}
