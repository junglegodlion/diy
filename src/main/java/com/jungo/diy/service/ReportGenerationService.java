package com.jungo.diy.service;

import com.jungo.diy.entity.GateWayDailyPerformanceEntity;
import com.jungo.diy.model.PerformanceResult;
import com.jungo.diy.model.SlowRequestRateModel;
import com.jungo.diy.response.UrlPerformanceResponse;
import com.jungo.diy.util.DateUtils;
import com.jungo.diy.util.TableUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlException;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDLbls;
import org.openxmlformats.schemas.drawingml.x2006.chart.STDLblPos;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.jungo.diy.util.DateUtils.MM_DD;
import static com.jungo.diy.util.DateUtils.YYYY_MM_DD;

/**
 * @author lichuang3
 */
@Service
@Slf4j
public class ReportGenerationService {

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
            template = new XWPFDocument(Objects.requireNonNull(ReportGenerationService.class.getResourceAsStream("/format.docx")));
            // 获得模板文档的整体样式
            wordStyles = template.getStyle();
        } catch (IOException | XmlException e) {
            log.error("WordDocumentGenerator#static initializer,出现异常！", e);
        }
    }

    @Autowired
    private AnalysisService analysisService;

    public String generateWordDocument(LocalDate startDate, LocalDate endDate) throws IOException, InvalidFormatException {
        // 新建目录，将文件保存在改目录下
        LocalDate currentDate = LocalDate.now();
        String formattedDate = DateUtils.getDateString(currentDate, YYYY_MM_DD);
        String directoryPath = System.getProperty("user.home") + "/Desktop/备份/c端网关接口性能统计/数据统计/输出/" + formattedDate;
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            // 创建所有必要的目录
            directory.mkdirs();
        }

        PerformanceResult result = analysisService.getPerformanceResult(startDate, endDate);
        // 生成网关性能变化曲线图
        generateGateWayPerformanceCurveChart(endDate, directoryPath);
        // 生成word文档
        generateWord(startDate, endDate, result, directoryPath);

        return directoryPath;
    }

    private void generateGateWayPerformanceCurveChart(LocalDate endDate, String directoryPath) throws UnsupportedEncodingException {
        LocalDate startDateForMonthPerformanceTrend = endDate.minusDays(30);
        XSSFWorkbook gateWayPerformanceCurveChart = analysisService.getGateWayPerformanceCurveChart(startDateForMonthPerformanceTrend);
        String fileName = URLEncoder.encode("gatewayPerformanceChangeCurveGraph.xlsx", StandardCharsets.UTF_8.toString());
        String filePath = directoryPath + "/" + fileName;
        /* 执行文件写入操作 */
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            gateWayPerformanceCurveChart.write(fileOut);
        } catch (IOException e) {
            log.error("ReportGenerationService#generateGateWayPerformanceCurveChart,出现异常！", e);
        }
    }

    private void generateWord(LocalDate startDate, LocalDate endDate, PerformanceResult result, String directoryPath) throws IOException {
        // 创建一个新的Word文档
        try (XWPFDocument document = new XWPFDocument()) {
            setWordStyle(document);

            // 第一部分：网关性能监控
            setFirstLevelTitle(document, "一、网关性能监控");
            List<Section> sections = Arrays.asList(
                    new Section("cl-gateway 月平均慢请求率",
                            d -> drawGatewayMonthlyAverageSlowRequestRateTable(d, result.getGatewayAverageSlowRequestRate())),
                    new Section(startDate, endDate, "大盘数据数据情况",
                            d -> {
                                drawWeeklyMarketDataSituationTable(d, result.getWeeklyMarketDataSituationData());
                                setText(d, "最近一周慢请求率均值：" + TableUtils.getPercentageFormatDouble(result.getAverageSlowRequestRateInThePastWeek()));
                            }),
                    new Section(endDate.minusDays(30), endDate, "99线趋势",
                            d -> insertLineChart(d, result.getMonthlySlowRequestRateTrendData(), "99线趋势", "日期", "毫秒", false)),
                    new Section(endDate.minusDays(30), endDate, "慢请求率趋势",
                            d -> insertLineChart(d, result.getMonthlySlowRequestRateTrendData(), "慢请求率趋势", "日期", "百分比", true)),
                    new Section(LocalDate.now().getYear() + "年周维度99线趋势",
                            d -> {/* 图片插入逻辑 */}),
                    new Section(LocalDate.now().getYear() + "年周维度慢请求率趋势",
                            d -> {/* 图片插入逻辑 */})
            );

            // 统一生成章节
            generateSections(document, sections, 1);

            // 第二部分：核心接口监控
            setFirstLevelTitle(document, "二、核心接口监控接口");
            generateCoreSections(document, result, startDate, endDate);
            // jungo TODO 2025/3/12:结论

            // 保存文档
            String fileName = URLEncoder.encode(  "performance.docx", StandardCharsets.UTF_8.toString());
            String filePath = directoryPath + "/" + fileName;
            try (FileOutputStream out = new FileOutputStream(filePath)) {
                document.write(out);
            }
        }
    }

    // 新增章节生成器
    private void generateSections(XWPFDocument doc, List<Section> sections, int chapter) {
        int index = 1;
        for (Section section : sections) {
            setSecondLevelTitle(doc, chapter + "." + index + " " + section.getTitle());
            section.execute(doc);
            index++;
        }
    }


    // 核心接口章节生成
    private void generateCoreSections(XWPFDocument doc, PerformanceResult result, LocalDate start, LocalDate end) {
        List<BiConsumer<XWPFDocument, PerformanceResult>> coreSections = Arrays.asList(
                (d, r) -> drawCriticalPathTable(d, r.getCriticalLinkUrlPerformanceResponses(), start, end),
                (d, r) -> drawTheFiveGreatVajrasTable(d, r.getFiveGangJingUrlPerformanceResponses(), start, end),
                (d, r) -> drawFirstScreenTabTable(d, r.getFirstScreenTabUrlPerformanceResponses(), start, end),
                (d, r) -> drawQilinComponentInterfaceTable(d, r.getQilinComponentInterfaceUrlPerformanceResponses(), start, end),
                (d, r) -> drawOtherCoreBusinessInterfaceTable(d, r.getOtherCoreBusinessInterfaceUrlPerformanceResponses(), start, end),
                (d, r) -> drawRequestVolumeTopInterfaceTable(d, r.getAccessVolumeTop30Interface(), start, end)
        );

        int index = 1;
        for (BiConsumer<XWPFDocument, PerformanceResult> section : coreSections) {
            setSecondLevelTitle(doc, "2." + index + " " + getCoreSectionTitle(index));
            section.accept(doc, result);
            index++;
        }
    }

    // 新增章节标题映射
    private String getCoreSectionTitle(int index) {
        switch (index) {
            case 1:
                return "关键路径";
            case 2:
                return "五大金刚";
            case 3:
                return "首屏TAB";
            case 4:
                return "活动页关键组件（麒麟组件接口）";
            case 5:
                return "其他核心业务接口";
            case 6:
                return "请求量TOP接口";
            default:
                return "";
        }
    }

    // 新增章节数据类
    @Data
    private static class Section {
        private String title;
        private Consumer<XWPFDocument> action;
        private LocalDate start;
        private LocalDate end;

        public Section(String title, LocalDate start, LocalDate end, Consumer<XWPFDocument> action) {
            this.title = title;
            this.action = action;
            this.start = start;
            this.end = end;
        }

        Section(String title, Consumer<XWPFDocument> action) {
            this(title, null, null, action);
        }

        Section(LocalDate start, LocalDate end, String title, Consumer<XWPFDocument> action) {
            this(title, start, end, action);
        }

        // 执行章节生成逻辑
        void execute(XWPFDocument doc) {
            this.action.accept(doc);
        }

        // ... 其他辅助方法 ...
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
        try (InputStream inputStream = ReportGenerationService.class.getResourceAsStream("/images/img.png")) {
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