
package com.jungo.diy.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.PresetColor;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDLbls;
import org.openxmlformats.schemas.drawingml.x2006.chart.STDLblPos;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

import static com.jungo.diy.util.ExcelChartGenerator.*;

/**
 * @author lichuang3
 * @date 2025-02-25 10:17
 */
public class TableUtils {

    private TableUtils() {}

    /**
     * 将给定的慢请求速率转换为百分比格式的字符串，保留两位小数。
     *
     * @param slowRequestRate 慢请求速率（小数形式）
     * @return 格式化后的百分比字符串
     */
    public static String getPercentageFormatDouble(double slowRequestRate) {
        // slowRequestRate转化是百分数，保留2位小数
        DecimalFormat df = new DecimalFormat("0.00%");
        // 可选：设置四舍五入模式（默认HALF_EVEN）
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(slowRequestRate);
    }

    /**
     * 在给定的 XWPFDocument 文档中创建一个带有指定行数和列数的 XWPFTable 表格。
     * 表格宽度占页面宽度的 100%，并且带有单线边框。
     *
     * @param document XWPFDocument 文档对象
     * @param rows 表格的行数
     * @param cols 表格的列数
     * @return 创建的 XWPFTable 表格对象
     */
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

    /**
     * 将给定的字符串转换为整数。
     * 如果字符串包含小数部分，则抛出 IllegalArgumentException。
     *
     * @param input 待转换的字符串
     * @return 转换后的整数
     * @throws IllegalArgumentException 如果输入包含非整数部分或无效的数字格式
     */
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

    /**
     * 在给定的 XSSFWorkbook 工作簿中创建一个名为 sheetName 的工作表，
     * 并使用提供的数据和图表配置填充该工作表。
     *
     * @param workbook XSSFWorkbook 工作簿对象
     * @param sheetName 工作表名称
     * @param models 数据模型列表
     * @param columnTitles 列标题数组
     * @param titleText 图表标题文本
     * @param xTitle X 轴标题文本
     * @param yTitle Y 轴标题文本
     * @param seriesTitle 数据系列标题文本
     * @param extractor 用于提取数据的 Extractor 接口实现
     */
    public static <P> void createModelSheet(XSSFWorkbook workbook,
                                            String sheetName,
                                            List<P> models,
                                            String[] columnTitles,
                                            String titleText,
                                            String xTitle,
                                            String yTitle,
                                            String seriesTitle,
                                            Extractor<P> extractor) {
        // 创建工作表
        XSSFSheet sheet = workbook.createSheet(sheetName);
        // 写入数据
        createChartData(workbook, sheet, models, columnTitles, extractor);
        // 创建绘图对象
        XSSFChart chart = createXssfChart(titleText, sheet);
        // 配置图表数据
        configurePerformanceLineChartData(chart, sheet, xTitle, yTitle, seriesTitle);
    }

    /**
     * 返回一个用于格式化百分比的 CellStyle 对象。
     * 百分比格式保留两位小数。
     *
     * @param workbook XSSFWorkbook 工作簿对象
     * @return 格式化的 CellStyle 对象
     */
    public static CellStyle getPercentageCellStyle(XSSFWorkbook workbook) {
        DataFormat dataFormat = workbook.createDataFormat();
        short percentageFormat = dataFormat.getFormat("0.00%");
        CellStyle percentageCellStyle = workbook.createCellStyle();
        percentageCellStyle.setDataFormat(percentageFormat);
        return percentageCellStyle;
    }

    /**
     * 配置给定的 XSSFChart 对象的性能折线图数据。
     * 图表将使用提供的 X 轴标题、Y 轴标题和数据系列标题。
     * Y 轴将被隐藏，并且数据标签仅显示 Y 轴数值。
     *
     * @param chart XSSFChart 图表对象
     * @param sheet XSSFSheet 工作表对象
     * @param xTitle X 轴标题文本
     * @param yTitle Y 轴标题文本
     * @param seriesTitle 数据系列标题文本
     */
    public static void configurePerformanceLineChartData(XSSFChart chart,
                                                         XSSFSheet sheet,
                                                         String xTitle,
                                                         String yTitle,
                                                         String seriesTitle) {
        // 1. 创建数据源引用
        int lastRowNum = sheet.getLastRowNum();
        CellRangeAddress categoryRange = new CellRangeAddress(1, lastRowNum, 0, 0);
        CellRangeAddress valueRange = new CellRangeAddress(1, lastRowNum, 1, 1);

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
        XDDFValueAxis yAxis = getValueAxis(chart, yTitle);
        // 新增：隐藏Y轴主体
        yAxis.setVisible(false);
        XDDFChartData data = chart.createData(
                ChartTypes.LINE,
                getChartAxis(chart, xTitle),
                yAxis
        );

        // 4. 添加数据系列
        XDDFChartData.Series series = data.addSeries(categories, values);
        series.setTitle(seriesTitle, null);
        setLineStyle(series, PresetColor.YELLOW);

        // XDDFChartLegend legend = chart.getOrAddLegend();
        // legend.setPosition(LegendPosition.TOP_RIGHT);

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
        // 标签位置设置为顶部
        // 尝试设置标签位置为顶部
        try {
            dLbls.addNewDLblPos().setVal(STDLblPos.T);
        } catch (Exception e) {
            // 处理异常，可能是由于模式文件缺失或其他原因
            System.err.println("Failed to set data label position: " + e.getMessage());
        }

        // 5. 绘制图表
        chart.plot(data);
    }

    /**
     * 在给定的 XSSFSheet 工作表中创建一个 XSSFChart 图表对象。
     * 图表的位置和大小由 XSSFClientAnchor 对象指定。
     *
     * @param titleText 图表标题文本
     * @param sheet XSSFSheet 工作表对象
     * @return 创建的 XSSFChart 图表对象
     */
    private static XSSFChart createXssfChart(String titleText, XSSFSheet sheet) {
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 3, 5, 13, 20);
        int chartWidthCols = (int) Math.ceil((sheet.getLastRowNum() - 1) * 0.5);
        int endCol = anchor.getCol1() + chartWidthCols;
        anchor.setCol2(anchor.getCol1() + endCol);
        // 4. 创建图表对象
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(titleText);
        chart.setTitleOverlay(false);
        return chart;
    }

    /**
     * 在给定的 XSSFSheet 工作表中创建图表数据。
     * 使用提供的数据模型列表、列标题数组和 Extractor 接口实现来填充数据。
     *
     * @param workbook XSSFWorkbook 工作簿对象
     * @param sheet XSSFSheet 工作表对象
     * @param models 数据模型列表
     * @param columnTitles 列标题数组
     * @param extractor 用于提取数据的 Extractor 接口实现
     */
    public static <P> void createChartData(XSSFWorkbook workbook, XSSFSheet sheet, List<P> models, String[] columnTitles, Extractor<P> extractor) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columnTitles.length; i++) {
            headerRow.createCell(i).setCellValue(columnTitles[i]);
        }

        // 创建数据行
        for (int i = 0; i < models.size(); i++) {
            Row row = sheet.createRow(i + 1);
            for (int j = 0; j < columnTitles.length; j++) {
                Cell cell = row.createCell(j);
                extractor.extract(models.get(i), j, cell);
            }
        }
    }

    // 定义一个提取器接口
    @FunctionalInterface
    public interface Extractor<P> {
        void extract(P model, int columnIndex, Cell cell);
    }
}
