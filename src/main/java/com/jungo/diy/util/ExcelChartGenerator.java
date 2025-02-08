package com.jungo.diy.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.*;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import java.io.IOException;
import java.io.OutputStream;

public class ExcelChartGenerator {

    public static void generateChartWithData(OutputStream outputStream) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // 1. 创建工作表
            XSSFSheet sheet = workbook.createSheet("销售数据");

            // 2. 写入示例数据
            createData(sheet);

            // 3. 创建绘图对象
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 3, 5, 13, 20);

            // 4. 创建图表对象
            XSSFChart chart = drawing.createChart(anchor);
            chart.setTitleText("月度销售额趋势");
            chart.setTitleOverlay(false);

            // 5. 配置图表数据
            configureChartData(workbook, chart, sheet);

            // 6. 保存文件
            workbook.write(outputStream);
        }
    }

    private static void createData(XSSFSheet sheet) {
        // 表头
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("月份");
        headerRow.createCell(1).setCellValue("销售额（万元）");

        // 数据行
        Object[][] data = {
            {"1月", 45},
            {"2月", 53},
            {"3月", 62},
            {"4月", 58},
            {"5月", 67},
            {"6月", 75}
        };

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue((String) data[i][0]);
            Number number = (Number) data[i][1];
            row.createCell(1).setCellValue(number.doubleValue());
        }
    }

    private static void configureChartData(
        XSSFWorkbook workbook, 
        XSSFChart chart, 
        XSSFSheet sheet
    ) {
        // 1. 创建数据源引用
        CellRangeAddress categoryRange = new CellRangeAddress(1, 6, 0, 0); // A2:A7
        CellRangeAddress valueRange = new CellRangeAddress(1, 6, 1, 1);    // B2:B7

        // 2. 创建数据源
        XDDFDataSource<String> categories = XDDFDataSourcesFactory.fromStringCellRange(
            sheet, 
            categoryRange
        );
        XDDFNumericalDataSource<Double> values = XDDFDataSourcesFactory.fromNumericCellRange(
            sheet, 
            valueRange
        );

        // 3. 创建图表数据
        XDDFChartData data = chart.createData(
            ChartTypes.LINE, 
            getChartAxis(chart), 
            getValueAxis(chart)
        );

        // 4. 添加数据系列
        XDDFChartData.Series series = data.addSeries(categories, values);
        series.setTitle("销售额", null);
        setLineStyle(series, PresetColor.BLUE);

        // 5. 绘制图表
        chart.plot(data);
    }

    private static XDDFCategoryAxis getChartAxis(XSSFChart chart) {
        XDDFCategoryAxis categoryAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        categoryAxis.setTitle("月份");
        return categoryAxis;
    }

    private static XDDFValueAxis getValueAxis(XSSFChart chart) {
        XDDFValueAxis valueAxis = chart.createValueAxis(AxisPosition.LEFT);
        valueAxis.setTitle("销售额");
        valueAxis.setCrosses(AxisCrosses.AUTO_ZERO);
        return valueAxis;
    }

    private static void setLineStyle(XDDFChartData.Series series, PresetColor color) {
        XDDFLineProperties lineProps = new XDDFLineProperties();
        lineProps.setWidth(1.5);
        // lineProps.setDashType(DashType.SOLID);

        XDDFSolidFillProperties fill = new XDDFSolidFillProperties(
            XDDFColor.from(color)
        );
        XDDFShapeProperties shape = new XDDFShapeProperties();
        shape.setFillProperties(fill);
        shape.setLineProperties(lineProps);

        series.setShapeProperties(shape);
    }
}