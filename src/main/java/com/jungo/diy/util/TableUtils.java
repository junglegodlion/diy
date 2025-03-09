package com.jungo.diy.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * @author lichuang3
 * @date 2025-02-25 10:17
 */
public class TableUtils {

    private TableUtils() {}

    public static String getPercentageFormatString(double slowRequestRate) {
        // slowRequestRate转化是百分数，保留2位小数
        DecimalFormat df = new DecimalFormat("0.00%");
        // 可选：设置四舍五入模式（默认HALF_EVEN）
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(slowRequestRate);
    }

    public static XWPFTable createXwpfTable(XWPFDocument document, int rows, int cols) {
        // 插入表格前创建段落
        XWPFParagraph tableParagraph = document.createParagraph();
        // 添加空行分隔
        tableParagraph.createRun().addBreak();
        XWPFTable table = document.createTable(rows, cols);
        // 设置表格宽度（占页面宽度的100%）
        table.setWidth("100%");
        // 设置表格边框（必须）
        CTTblBorders borders = table.getCTTbl().addNewTblPr().addNewTblBorders();
        borders.addNewBottom().setVal(STBorder.SINGLE);
        borders.addNewLeft().setVal(STBorder.SINGLE);
        borders.addNewRight().setVal(STBorder.SINGLE);
        borders.addNewTop().setVal(STBorder.SINGLE);
        borders.addNewInsideH().setVal(STBorder.SINGLE);
        borders.addNewInsideV().setVal(STBorder.SINGLE);
        return table;
    }

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
