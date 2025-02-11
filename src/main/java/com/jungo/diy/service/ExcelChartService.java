package com.jungo.diy.service;

import com.jungo.diy.model.P99Model;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDLbls;
import org.springframework.stereotype.Service;



import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExcelChartService {

    public static XSSFWorkbook generateExcelWithChart(List<P99Model> averageP99Models) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        
        // 1. 创建数据表
        XSSFSheet dataSheet = workbook.createSheet("99线周维度");
        fillTestData(dataSheet, averageP99Models); // 填充测试数据
        
        // 2. 创建图表表
        XSSFSheet chartSheet = workbook.createSheet("99线周维度-图表");
        createLineChart(dataSheet, chartSheet);
        
        return workbook;
    }

    private static void fillTestData(XSSFSheet sheet, List<P99Model> averageP99Models) {
        // 填充示例数据（同之前的实现）
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("日期");
        headerRow.createCell(1).setCellValue("99线");

        // 填充数据行
        List<String> dates = averageP99Models.stream().map(P99Model::getDate).collect(Collectors.toList());
        List<Integer> p99Values = averageP99Models.stream().map(P99Model::getP99).collect(Collectors.toList());

        for (int i = 0; i < dates.size(); i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(dates.get(i));
            row.createCell(1).setCellValue(p99Values.get(i));
        }
    }

    private static void createLineChart(XSSFSheet dataSheet, XSSFSheet chartSheet) {
        XSSFDrawing drawing = chartSheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 5, 15, 25);
        
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("99线周维度-图表");
        
        // 配置坐标轴
        XDDFCategoryAxis xAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis yAxis = chart.createValueAxis(AxisPosition.LEFT);
        yAxis.setTitle("99线");
        // 配置横坐标
        xAxis.setTitle("日期");
        
        // 定义数据范围
        int lastRowNum = dataSheet.getLastRowNum();
        XDDFDataSource<String> xData = XDDFDataSourcesFactory.fromStringCellRange(dataSheet,
            new CellRangeAddress(1, lastRowNum, 0, 0));
        XDDFNumericalDataSource<Double> yData = XDDFDataSourcesFactory.fromNumericCellRange(dataSheet,
                new CellRangeAddress(1, lastRowNum, 1, 1));

        // 创建折线图数据
        XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, xAxis, yAxis);
        XDDFLineChartData.Series series = (XDDFLineChartData.Series) data.addSeries(xData, yData);

        chart.plot(data);
        // POI 5.2.3 及以上，启用数据标签的正确方式
        // **仅显示数据点的 Y 轴数值（不显示类别名、序列名等）**
        CTDLbls dLbls = chart.getCTChart().getPlotArea().getLineChartArray(0).getSerArray(0).addNewDLbls();
        // 仅显示数值
        dLbls.addNewShowVal().setVal(true);
        // 不显示图例键
        dLbls.addNewShowLegendKey().setVal(false);
        // 不显示类别名称
        dLbls.addNewShowCatName().setVal(false);
        dLbls.addNewShowSerName().setVal(false);

    }
}
