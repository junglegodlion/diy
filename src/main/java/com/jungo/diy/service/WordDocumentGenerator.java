package com.jungo.diy.service;

import com.jungo.diy.entity.GateWayDailyPerformanceEntity;
import com.jungo.diy.enums.InterfaceTypeEnum;
import com.jungo.diy.model.PerformanceResult;
import com.jungo.diy.model.SlowRequestRateModel;
import com.jungo.diy.model.UrlPerformanceModel;
import com.jungo.diy.repository.PerformanceRepository;
import com.jungo.diy.response.UrlPerformanceResponse;
import com.jungo.diy.util.DateUtils;
import com.jungo.diy.util.TableUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDLbls;
import org.openxmlformats.schemas.drawingml.x2006.chart.STDLblPos;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyles;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jungo.diy.util.DateUtils.MM_DD;
import static com.jungo.diy.util.DateUtils.YYYY_MM_DD;

/**
 * @author lichuang3
 */
@Service
@Slf4j
public class WordDocumentGenerator {

    // 新增常量类定义列索引（避免硬编码）
    private static class TableColumns {
        public static final int PAGE_NAME = 0;
        public static final int URL = 1;
        public static final int LAST_WEEK_P99 = 2;
        public static final int THIS_WEEK_P99 = 3;
        public static final int LAST_WEEK_COUNT = 4;
        public static final int THIS_WEEK_COUNT = 5;
        public static final int LAST_WEEK_SLOW_RATE = 6;
        public static final int THIS_WEEK_SLOW_RATE = 7;
        public static final int P99_CHANGE = 8;
        public static final int P99_CHANGE_RATE = 9;
        // 仅关键链路表使用
        public static final int P99_TARGET = 10;
        // 仅关键链路表使用
        public static final int REACH_TARGET = 11;
    }

    /**
     * word整体样式
     */
    private static CTStyles wordStyles = null;

    /**
     * Word整体样式
     */
    static {
        XWPFDocument template;
        try {
            // 读取模板文档
            // 读取resources的文件
            template = new XWPFDocument(Objects.requireNonNull(WordDocumentGenerator.class.getResourceAsStream("/format.docx")));
            // 获得模板文档的整体样式
            wordStyles = template.getStyle();
        } catch (IOException | XmlException e) {
            log.error("WordDocumentGenerator#static initializer,出现异常！", e);
        }
    }
    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private AnalysisService analysisService;

    public void generateWordDocument(String filePath,
                                     LocalDate startDate,
                                     LocalDate endDate) throws IOException, InvalidFormatException {
        /*获取表格数据*/
        // 月平均慢请求率
        List<SlowRequestRateModel> gatewayAverageSlowRequestRate = performanceRepository.getGatewayAverageSlowRequestRate(LocalDate.now().getYear());
        // 获取周大盘数据数据情况
        List<GateWayDailyPerformanceEntity> weeklyMarketDataSituationData = performanceRepository.getWeeklyMarketDataSituationTable(startDate, endDate);
        // weeklyMarketDataSituationData的平均值
        double averageSlowRequestRateInThePastWeek = weeklyMarketDataSituationData.stream().mapToDouble(GateWayDailyPerformanceEntity::getSlowRequestRate).average().orElse(0.0);

        /*月慢请求率趋势数据*/
        // endDate前30天
        LocalDate startDateForMonthSlowRequestRateTrend = endDate.minusDays(30);
        List<GateWayDailyPerformanceEntity> monthlySlowRequestRateTrendData = performanceRepository.getMonthlySlowRequestRateTrendData(startDateForMonthSlowRequestRateTrend, endDate);

        PerformanceResult result = analysisService.getPerformanceResult(startDate, endDate);

        // 创建一个新的Word文档
        try (XWPFDocument document = new XWPFDocument()) {
            setWordStyle(document);

            /*cl-gateway 月平均慢请求率*/
            setFirstLevelTitle(document, "cl-gateway 月平均慢请求率");
            drawGatewayMonthlyAverageSlowRequestRateTable(document, gatewayAverageSlowRequestRate);

            /*周大盘数据数据情况*/
            String startDateStr = DateUtils.getDateString(startDate, YYYY_MM_DD);
            String endDateStr = DateUtils.getDateString(endDate, YYYY_MM_DD);
            setFirstLevelTitle(document, startDateStr + "~" + endDateStr + " 大盘数据数据情况");
            drawWeeklyMarketDataSituationTable(document, weeklyMarketDataSituationData);
            setText(document, "最近一周慢请求率均值：" + TableUtils.getPercentageFormatDouble(averageSlowRequestRateInThePastWeek));

            /*月慢请求率趋势*/
            setFirstLevelTitle(document, startDateForMonthSlowRequestRateTrend + "~" + endDateStr + " 慢请求率趋势");
            // 生成折线图
            insertLineChart(document, monthlySlowRequestRateTrendData, "慢请求率趋势", "日期", "百分比", true);

            // jungo TODO 2025/3/10:补充图片


            /*月99线趋势*/
            setFirstLevelTitle(document, startDateForMonthSlowRequestRateTrend + "~" + endDateStr + " 99线趋势");
            insertLineChart(document, monthlySlowRequestRateTrendData, "99线趋势", "日期", "毫秒", false);
            // jungo TODO 2025/3/10:补充图片

            /*2025年周维度99线趋势*/
            int year = LocalDate.now().getYear();
            setFirstLevelTitle(document, year + "年周维度99线趋势");
            // jungo TODO 2025/3/10:补充图片

            /* 2025年周维度慢请求率趋势*/
            setFirstLevelTitle(document, year + "年周维度慢请求率趋势");
            // jungo TODO 2025/3/10:补充图片

            /*核心接口监控接口*/
            setFirstLevelTitle(document, "核心接口监控接口");
            setSecondLevelTitle(document, "一、关键路径");
            drawCriticalPathTable(document, result.getCriticalLinkUrlPerformanceResponses(), startDate, endDate);
            setSecondLevelTitle(document, "二、五大金刚");
            drawTheFiveGreatVajrasTable(document, result.getFiveGangJingUrlPerformanceResponses(), startDate, endDate);
            setSecondLevelTitle(document, "三、首屏TAB");
            drawFirstScreenTabTable(document, result.getFirstScreenTabUrlPerformanceResponses(), startDate, endDate);
            setSecondLevelTitle(document, "四、活动页关键组件（麒麟组件接口）");
            drawQilinComponentInterfaceTable(document, result.getQilinComponentInterfaceUrlPerformanceResponses(), startDate, endDate);
            setSecondLevelTitle(document, "五、其他核心业务接口");
            drawOtherCoreBusinessInterfaceTable(document, result.getOtherCoreBusinessInterfaceUrlPerformanceResponses(), startDate, endDate);
            setSecondLevelTitle(document, "六、请求量TOP接口");
            drawRequestVolumeTopInterfaceTable(document, result.getAccessVolumeTop30Interface(), startDate, endDate);

            setImage(document);
            // 保存文档
            try (FileOutputStream out = new FileOutputStream(filePath)) {
                document.write(out);
            }
        }



    }

    // 新增POI图表插入方法
    private void insertLineChart(XWPFDocument doc,
                                 List<GateWayDailyPerformanceEntity> data,
                                 String title,
                                 String categoryAxisLabel,
                                 String valueAxisLabel,
                                 boolean isPercentage) {
        // 创建图表段落
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        // 图表标题占位文本
        r.setText(title);
        r.addBreak();

        // 创建图表
        XWPFChart chart = null;
        try {
            int chartWidthCols = (int) Math.ceil((data.size() - 1) * 0.5);
            chart = doc.createChart(r, chartWidthCols * Units.EMU_PER_CENTIMETER, 10 * Units.EMU_PER_CENTIMETER);
        } catch (InvalidFormatException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 配置图表数据
        String[] categories = data.stream()
                .map(e -> DateUtils.getDateString(e.getDate(), MM_DD))
                .toArray(String[]::new);

        Double[] values = data.stream()
                .map(e -> isPercentage ? e.getSlowRequestRate() * 100 : (double)e.getP99())
                .toArray(Double[]::new);

        // 创建数据源
        XDDFCategoryDataSource categoryDS = XDDFDataSourcesFactory.fromArray(categories);
        XDDFNumericalDataSource<Double> valueDS = XDDFDataSourcesFactory.fromArray(values);

        // 创建轴时保存引用（原代码修改）
        XDDFCategoryAxis categoryAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis valueAxis = chart.createValueAxis(AxisPosition.LEFT);
        // 创建折线图数据
        XDDFLineChartData lineChartData = (XDDFLineChartData) chart.createData(
                ChartTypes.LINE,
                categoryAxis,
                valueAxis
        );

        // 添加系列数据
        XDDFLineChartData.Series series = (XDDFLineChartData.Series) lineChartData.addSeries(
                categoryDS,
                valueDS
        );
        series.setTitle("趋势", null);
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

        // 应用数据到图表
        chart.plot(lineChartData);


        // 设置Y轴显示百分比格式
        // 设置数值轴格式（原报错位置修改）
        valueAxis.setCrosses(AxisCrosses.AUTO_ZERO);
        valueAxis.setNumberFormat(isPercentage ? "0%" : "0");
    }
    private void drawRequestVolumeTopInterfaceTable(XWPFDocument document,
                                                    List<UrlPerformanceResponse> responses,
                                                    LocalDate startDate,
                                                    LocalDate endDate) {
        drawCommonTable(document, responses, startDate, endDate, false);
    }

    private void drawOtherCoreBusinessInterfaceTable(XWPFDocument document,
                                                     List<UrlPerformanceResponse> responses,
                                                     LocalDate startDate,
                                                     LocalDate endDate) {


        drawCommonTable(document, responses, startDate, endDate, false);
    }

    private void drawQilinComponentInterfaceTable(XWPFDocument document,
                                                  List<UrlPerformanceResponse> responses,
                                                  LocalDate startDate,
                                                  LocalDate endDate) {


        drawCommonTable(document, responses, startDate, endDate, false);
    }

    private void drawFirstScreenTabTable(XWPFDocument document,
                                         List<UrlPerformanceResponse> responses,
                                         LocalDate startDate,
                                         LocalDate endDate) {
        drawCommonTable(document, responses, startDate, endDate, false);

    }

    // 通用绘制方法
    private void drawCommonTable(XWPFDocument document,
                                 List<UrlPerformanceResponse> dataList,
                                 LocalDate startDate,
                                 LocalDate endDate,
                                 boolean isCriticalPath) { // 标识是否关键链路表

        // 日期格式化提取到公共方法
        String[] dateStrings = getFormattedDates(startDate, endDate);

        // 动态构建表头
        List<String> headerList = new ArrayList<>(Arrays.asList(
                "页面名称",
                "接口",
                dateStrings[0] + "日99线",
                dateStrings[1] + "日99线",
                dateStrings[0] + "日调用量",
                dateStrings[1] + "日调用量",
                dateStrings[0] + "慢请求(300ms)率",
                dateStrings[1] + "慢请求(300ms)率",
                "接口性能变化（ms）",
                "99线环比"
        ));

        // 关键链路表追加特殊列
        if (isCriticalPath) {
            headerList.addAll(Arrays.asList(
                    "99线基线目标",
                    "是否达到目标"
            ));
        }

        // 执行绘制
        drawTable(document, dataList, headerList.toArray(new String[0]), (row, entity) -> {
            // 空指针防御
            if (row == null || entity == null) {
                return;
            }

            // 公共字段填充
            fillCommonCells(row, entity);

            // 关键链路表特殊处理
            if (isCriticalPath) {
                row.getCell(TableColumns.P99_TARGET).setText(String.valueOf(entity.getP99Target()));
                row.getCell(TableColumns.REACH_TARGET).setText(String.valueOf(entity.getReachTarget()));
            }
        });
    }

    // 公共字段填充方法
    private void fillCommonCells(XWPFTableRow row, UrlPerformanceResponse entity) {
        row.getCell(TableColumns.PAGE_NAME).setText(entity.getPageName());
        row.getCell(TableColumns.URL).setText(entity.getUrl());
        row.getCell(TableColumns.LAST_WEEK_P99).setText(String.valueOf(entity.getLastWeekP99()));
        row.getCell(TableColumns.THIS_WEEK_P99).setText(String.valueOf(entity.getThisWeekP99()));
        row.getCell(TableColumns.LAST_WEEK_COUNT).setText(String.valueOf(entity.getLastWeekTotalRequestCount()));
        row.getCell(TableColumns.THIS_WEEK_COUNT).setText(String.valueOf(entity.getThisWeekTotalRequestCount()));
        row.getCell(TableColumns.LAST_WEEK_SLOW_RATE).setText(TableUtils.getPercentageFormatDouble(entity.getLastWeekSlowRequestRate()));
        row.getCell(TableColumns.THIS_WEEK_SLOW_RATE).setText(TableUtils.getPercentageFormatDouble(entity.getThisWeekSlowRequestRate()));
        row.getCell(TableColumns.P99_CHANGE).setText(String.valueOf(entity.getP99Change()));
        row.getCell(TableColumns.P99_CHANGE_RATE).setText(TableUtils.getPercentageFormatDouble(entity.getP99ChangeRate()));
    }

    // 日期格式化工具方法
    private String[] getFormattedDates(LocalDate start, LocalDate end) {
        return new String[]{
                DateUtils.getDateString(start, MM_DD),
                DateUtils.getDateString(end, MM_DD)
        };
    }

    // 原方法改造为
    private void drawTheFiveGreatVajrasTable(XWPFDocument document,
                                             List<UrlPerformanceResponse> responses,
                                             LocalDate startDate,
                                             LocalDate endDate) {
        drawCommonTable(document, responses, startDate, endDate, false);
    }

    private void drawCriticalPathTable(XWPFDocument document,
                                       List<UrlPerformanceResponse> responses,
                                       LocalDate startDate,
                                       LocalDate endDate) {
        drawCommonTable(document, responses, startDate, endDate, true);
    }


    // 新增通用表格生成方法
    private <T> void drawTable(XWPFDocument document,
                               List<T> data,
                               String[] headers,
                               BiConsumer<XWPFTableRow, T> rowDataSetter) {
        int rows = data.size();
        int cols = headers.length;
        XWPFTable table = TableUtils.createXwpfTable(document, rows + 1, cols);

        // 设置表头
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < cols; i++) {
            headerRow.getCell(i).setText(headers[i]);
        }

        // 填充表格内容
        for (int i = 0; i < rows; i++) {
            XWPFTableRow row = table.getRow(i + 1);
            rowDataSetter.accept(row, data.get(i));
        }
    }

    private void drawWeeklyMarketDataSituationTable(XWPFDocument document, List<GateWayDailyPerformanceEntity> weeklyMarketDataSituationData) {
        String[] headers = {"日期", "999线", "99线", "90线", "75线", "50线", "总请求数", "慢请求数", "慢请求率"};

        drawTable(document, weeklyMarketDataSituationData, headers, (row, entity) -> {
            row.getCell(0).setText(DateUtils.getDateString(entity.getDate(), YYYY_MM_DD));
            row.getCell(1).setText(String.valueOf(entity.getP999()));
            row.getCell(2).setText(String.valueOf(entity.getP99()));
            row.getCell(3).setText(String.valueOf(entity.getP90()));
            row.getCell(4).setText(String.valueOf(entity.getP75()));
            row.getCell(5).setText(String.valueOf(entity.getP50()));
            row.getCell(6).setText(String.valueOf(entity.getTotalRequestCount()));
            row.getCell(7).setText(String.valueOf(entity.getSlowRequestCount()));
            row.getCell(8).setText(TableUtils.getPercentageFormatDouble(entity.getSlowRequestRate()));
        });
    }


    private static void setWordStyle(XWPFDocument document) {
        // 获取新建文档对象的样式
        XWPFStyles newStyles = document.createStyles();
        // 关键行// 修改设置文档样式为静态块中读取到的样式
        // 参考：https://blog.csdn.net/hollycloud/article/details/120453185
        newStyles.setStyles(wordStyles);
    }

    private static void drawGatewayMonthlyAverageSlowRequestRateTable(XWPFDocument document, List<SlowRequestRateModel> gatewayAverageSlowRequestRate) {
        // 获取当前月份
        int currentMonth = LocalDate.now().getMonthValue();
        XWPFTable table = TableUtils.createXwpfTable(document, 2, currentMonth);
        // 设置表格表头
        XWPFTableRow headerRow = table.getRow(0);
        int year = LocalDate.now().getYear();
        for (int i = 0; i < currentMonth; i++) {
            String title = year + "-" + (i + 1);
            headerRow.getCell(i).setText(title);
        }

        XWPFTableRow row = table.getRow(1);
        // 填充表格内容
        for (int i = 0; i < currentMonth; i++) {
            double slowRequestRate = gatewayAverageSlowRequestRate.get(i).getSlowRequestRate();
            String format = TableUtils.getPercentageFormatDouble(slowRequestRate);
            row.getCell(i).setText(format);
        }
    }

    private static void setImage(XWPFDocument document) throws InvalidFormatException {
        // 创建图片段落
        XWPFParagraph imageParagraph = document.createParagraph();
        imageParagraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun imageRun = imageParagraph.createRun();
        try (InputStream inputStream = WordDocumentGenerator.class.getResourceAsStream("/images/img.png")) {
            if (inputStream == null) {
                throw new IOException("图片资源未找到：/images/img.png");
            }
            BufferedInputStream bufferedStream = new BufferedInputStream(inputStream);
            // 关键点：设置标记
            bufferedStream.mark(Integer.MAX_VALUE);

            BufferedImage originalImage = ImageIO.read(bufferedStream);
            bufferedStream.reset();  // 关键点：重置到标记位置

            imageRun.addPicture(
                    bufferedStream,
                    XWPFDocument.PICTURE_TYPE_PNG,
                    "img.png",
                    Units.toEMU(originalImage.getWidth() / 72f * 2.54) * 6,
                    Units.toEMU(originalImage.getHeight() / 72f * 2.54) * 6
            );
            System.out.println("原始尺寸：" + originalImage.getWidth() + "x" + originalImage.getHeight());
        } catch (IOException e) {
            throw new RuntimeException("图片加载失败", e);
        }
    }

    private static void setText(XWPFDocument document, String text) {
        // 创建段落
        XWPFParagraph paragraph = document.createParagraph();
        // 创建文本运行
        XWPFRun run = paragraph.createRun();
        run.setText(text);
    }

    private static void setTitle(XWPFDocument document, String title, String style, int fontSize) {
        // 创建段落
        XWPFParagraph titleParagraph = document.createParagraph();
        // 设置指定样式
        titleParagraph.setStyle(style);
        // 设置段落居中对齐
        titleParagraph.setAlignment(ParagraphAlignment.CENTER);
        // 创建文本运行
        XWPFRun titleRun = titleParagraph.createRun();
        titleRun.setText(title);
        // 加粗
        titleRun.setBold(true);
        // 设置字体大小
        titleRun.setFontSize(fontSize);
        // 添加换行
        titleRun.addBreak(BreakType.TEXT_WRAPPING);
    }

    private static void setFirstLevelTitle(XWPFDocument document, String title) {
        setTitle(document, title, "2", 22);
    }

    private void setSecondLevelTitle(XWPFDocument document, String title) {
        setTitle(document, title, "3", 22);
    }

}