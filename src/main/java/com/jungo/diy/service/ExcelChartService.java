package com.jungo.diy.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDLblPos;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDLbls;
import org.openxmlformats.schemas.drawingml.x2006.chart.STDLblPos;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBodyProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph;
import org.springframework.stereotype.Service;



import java.io.IOException;

@Service
public class ExcelChartService {

    public static XSSFWorkbook generateExcelWithChart() throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        
        // 1. 创建数据表
        XSSFSheet dataSheet = workbook.createSheet("销售数据");
        fillTestData(dataSheet); // 填充测试数据
        
        // 2. 创建图表表
        XSSFSheet chartSheet = workbook.createSheet("图表");
        createLineChart(workbook, dataSheet, chartSheet);
        
        return workbook;
    }

    private static void fillTestData(XSSFSheet sheet) {
        // 填充示例数据（同之前的实现）
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("月份");
        headerRow.createCell(1).setCellValue("销售额");
        
        String[] months = {"1月", "2月", "3月", "4月"};
        double[] sales = {45000, 52000, 48000, 61000};
        
        for (int i = 0; i < months.length; i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(months[i]);
            row.createCell(1).setCellValue(sales[i]);
        }
    }

    private static void createLineChart(XSSFWorkbook workbook, XSSFSheet dataSheet, XSSFSheet chartSheet) {
        XSSFDrawing drawing = chartSheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 5, 15, 25);
        
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("月度销售趋势");
        
        // 配置坐标轴
        XDDFCategoryAxis xAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis yAxis = chart.createValueAxis(AxisPosition.LEFT);
        yAxis.setTitle("销售额");
        
        // 定义数据范围
        XDDFDataSource<String> xData = XDDFDataSourcesFactory.fromStringCellRange(dataSheet,
            new CellRangeAddress(1, 4, 0, 0));
        XDDFNumericalDataSource<Double> yData = XDDFDataSourcesFactory.fromNumericCellRange(dataSheet,
                new CellRangeAddress(1, 4, 1, 1));

        // 创建折线图数据
        XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, xAxis, yAxis);
        XDDFLineChartData.Series series = (XDDFLineChartData.Series) data.addSeries(xData, yData);

        // 使用 CellReference 替代 XSSFCell
        CellReference cellReference = new CellReference(dataSheet.getRow(0).getCell(1).getRowIndex(), dataSheet.getRow(0).getCell(1).getColumnIndex());
        series.setTitle("销售额趋势", cellReference);
        // 绘制图表
        chart.plot(data);
        // POI 5.2.3 及以上，启用数据标签的正确方式
        // **仅显示数据点的 Y 轴数值（不显示类别名、序列名等）**
        CTDLbls dLbls = chart.getCTChart().getPlotArea().getLineChartArray(0).getSerArray(0).addNewDLbls();
        dLbls.addNewShowVal().setVal(true);  // 仅显示数值
        dLbls.addNewShowLegendKey().setVal(false);  // 不显示图例键
        dLbls.addNewShowCatName().setVal(false);  // 不显示类别名称
        dLbls.addNewShowSerName().setVal(false);
        // 设置标签位置（上方）
        // dLbls.addNewDLblPos().setVal(org.openxmlformats.schemas.drawingml.x2006.chart.STDLblPos.T);
    }
}
