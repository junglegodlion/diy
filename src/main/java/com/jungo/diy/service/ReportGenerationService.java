package com.jungo.diy.service;

import com.jungo.diy.entity.GateWayDailyPerformanceEntity;
import com.jungo.diy.enums.InterfaceTypeEnum;
import com.jungo.diy.model.PerformanceResult;
import com.jungo.diy.model.SlowRequestRateModel;
import com.jungo.diy.response.UrlPerformanceResponse;
import com.jungo.diy.util.DateUtils;
import com.jungo.diy.util.TableUtils;
import com.jungo.diy.util.TokenUtils;
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
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSimpleField;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        public static final int LAST_WEEK_P90 = 4;
        public static final int THIS_WEEK_P90 = 5;
        public static final int LAST_WEEK_COUNT = 6;
        public static final int THIS_WEEK_COUNT = 7;
        public static final int LAST_WEEK_SLOW_RATE = 8;
        public static final int THIS_WEEK_SLOW_RATE = 9;
        public static final int P99_CHANGE = 10;
        public static final int P99_CHANGE_RATE = 11;
        // 仅关键链路表使用
        public static final int P99_TARGET = 12;
        // 仅关键链路表使用
        public static final int REACH_TARGET = 13;
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
        validateDateRange(startDate, endDate);

        Path outputDir = prepareOutputDirectory();
        PerformanceResult result = analysisService.getPerformanceResult(startDate, endDate);

        generateAllCharts(result, endDate, outputDir);
        generateReportDocument(startDate, endDate, result, outputDir);

        return outputDir.toString();
    }

    private Path prepareOutputDirectory() throws IOException {
        Path desktopPath = Paths.get(System.getProperty("user.home"), "Desktop");
        Path outputPath = desktopPath.resolve("备份/c端网关接口性能统计/数据统计/输出")
                .resolve(LocalDate.now().format(DateTimeFormatter.ISO_DATE));

        Files.createDirectories(outputPath);
        return outputPath;
    }


    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
    }

    private void generateAllCharts(PerformanceResult result, LocalDate endDate, Path outputDir) {
        try {
            generateGateWayPerformanceCurveChart(endDate, outputDir.toString());
            generateTrendChartNonCompliantInterfaces(result, endDate, outputDir.toString());
        } catch (IOException e) {
            throw new RuntimeException("图表生成失败", e);
        }
    }

    private void generateReportDocument(LocalDate startDate,
                                        LocalDate endDate,
                                        PerformanceResult result,
                                        Path outputDir) {
        try {
            generateWord(startDate, endDate, result, outputDir.toString());
        } catch (IOException e) {
            throw new RuntimeException("文档生成失败", e);
        }
    }


    private void generateTrendChartNonCompliantInterfaces(PerformanceResult result, LocalDate endDate, String directoryPath) {
        LocalDate startDateForMonthPerformanceTrend = endDate.minusDays(30);

        // 统一处理所有接口类型
        List<String> nonCompliantUrls = Arrays.stream(InterfaceTypeEnum.values())
                .map(type -> extractNonCompliantUrls(result, type))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        try (XSSFWorkbook workbook = analysisService.batchGet99LineCurve(nonCompliantUrls.toArray(new String[0]), startDateForMonthPerformanceTrend)) {
            String fileName = URLEncoder.encode("NonCompliantInterfaces.xlsx", StandardCharsets.UTF_8.toString());
            saveWorkbookToFile(workbook, directoryPath, fileName);
        } catch (IOException e) {
            log.error("Failed to generate trend chart for non-compliant interfaces", e);
            throw new UncheckedIOException("Error generating trend chart file", e);
        }
    }

    /**
     * 提取指定类型的未达标 URL 列表
     */
    private List<String> extractNonCompliantUrls(PerformanceResult result, InterfaceTypeEnum type) {
        return Optional.ofNullable(getPerformanceResponses(result, type))
                // 防止 NullPointerException
                .orElse(Collections.emptyList())
                .stream()
                .filter(x -> Boolean.FALSE.equals(x.getReachTarget()))
                .map(x -> TokenUtils.generateToken(x.getHost(), x.getUrl()))
                .collect(Collectors.toList());
    }

    /**
     * 根据类型获取 PerformanceResult 中的 URL 响应列表
     */
    private List<UrlPerformanceResponse> getPerformanceResponses(PerformanceResult result, InterfaceTypeEnum type) {
        switch (type) {
            case CRITICAL_LINK:
                return result.getCriticalLinkUrlPerformanceResponses();
            case FIVE_GANG_JING:
                return result.getFiveGangJingUrlPerformanceResponses();
            case FIRST_SCREEN_TAB:
                return result.getFirstScreenTabUrlPerformanceResponses();
            case QILIN_COMPONENT_INTERFACE:
                return result.getQilinComponentInterfaceUrlPerformanceResponses();
            case OTHER_CORE_BUSINESS_INTERFACE:
                return result.getOtherCoreBusinessInterfaceUrlPerformanceResponses();
            case ACCESS_VOLUME_TOP30:
                return result.getAccessVolumeTop30Interface();
            default:
                return Collections.emptyList();
        }
    }

    // 优化文件保存逻辑（复用该方法）
    private void saveWorkbookToFile(XSSFWorkbook workbook, String directoryPath, String fileName) {
        String filePath = directoryPath + "/" + fileName;
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        } catch (IOException e) {
            log.error("文件保存失败 | 路径: {} | 错误: {}", filePath, e.getMessage(), e);
        }
    }


    private void generateGateWayPerformanceCurveChart(LocalDate endDate, String directoryPath) throws UnsupportedEncodingException {
        LocalDate startDateForMonthPerformanceTrend = endDate.minusDays(30);
        XSSFWorkbook gateWayPerformanceCurveChart = analysisService.getGateWayPerformanceCurveChart(startDateForMonthPerformanceTrend);
        String fileName = URLEncoder.encode("gatewayPerformanceChangeCurveGraph.xlsx", StandardCharsets.UTF_8.toString());
        saveWorkbookToFile(gateWayPerformanceCurveChart, directoryPath, fileName);
    }

    private void generateWord(LocalDate startDate, LocalDate endDate, PerformanceResult result, String directoryPath) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            setWordStyle(document);

            // 第一部分：网关性能监控
            setFirstLevelTitle(document, "一、(cl-gateway)网关性能监控");
            generateGatewayPerformanceSection(document, result, startDate, endDate);

            // 第二部分：核心接口监控
            setFirstLevelTitle(document, "二、核心接口监控接口");
            generateCoreSections(document, result, startDate, endDate);

            // 第三部分：结论
            setFirstLevelTitle(document, "三、结论");
            generateConclusionSection(document, result, startDate, endDate);

            // 保存 Word 文档
            saveWordDocument(document, directoryPath);
        }
    }

    /**
     * 生成网关性能监控部分
     */
    private void generateGatewayPerformanceSection(XWPFDocument document, PerformanceResult result, LocalDate startDate, LocalDate endDate) {
        List<Section> sections = Arrays.asList(
                new Section("cl-gateway 月平均慢请求率", d ->
                        drawGatewayMonthlyAverageSlowRequestRateTable(d, result.getGatewayAverageSlowRequestRate())
                ),
                new Section(startDate, endDate, "cl-gateway 大盘数据情况（最近7天）", d -> {
                    drawWeeklyMarketDataSituationTable(d, result.getWeeklyMarketDataSituationData());
                    setText(d, "最近一周慢请求率均值：" + TableUtils.getPercentageFormatDouble(result.getAverageSlowRequestRateInThePastWeek()));
                }),
                new Section(endDate.minusDays(30), endDate, "cl-gateway 大盘 99线趋势（最近30天）", d ->
                        insertLineChart(d, result.getMonthlySlowRequestRateTrendData(), "99线趋势", "日期", "毫秒", false)
                ),
                new Section(endDate.minusDays(30), endDate, "cl-gateway 大盘 慢请求率趋势（最近30天）", d ->
                        insertLineChart(d, result.getMonthlySlowRequestRateTrendData(), "慢请求率趋势", "日期", "百分比", true)
                ),
                new Section(LocalDate.now().getYear() + "年cl-gateway大盘99线趋势-周维度", d -> {/* 图片插入逻辑 */}),
                new Section(LocalDate.now().getYear() + "年cl-gateway大盘慢请求率趋势-周维度", d -> {/* 图片插入逻辑 */})
        );

        generateSections(document, sections, 1);
    }

    /**
     * 生成结论部分
     */
    private void generateConclusionSection(XWPFDocument document, PerformanceResult result, LocalDate startDate, LocalDate endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M.d");
        String dateRange = startDate.format(formatter) + "~" + endDate.format(formatter);
        int monthValue = LocalDate.now().getMonthValue();

        // 处理核心接口（关键路径）
        String criticalIssues = collectNonCompliantCriticalLinkUrls(
                result.getCriticalLinkUrlPerformanceResponses(),
                "");

        // 处理其他性能恶化的接口
        List<UrlPerformanceResponse> otherIssuesResponses = Stream.of(
                result.getFiveGangJingUrlPerformanceResponses(),
                result.getFirstScreenTabUrlPerformanceResponses(),
                result.getQilinComponentInterfaceUrlPerformanceResponses(),
                result.getOtherCoreBusinessInterfaceUrlPerformanceResponses(),
                result.getAccessVolumeTop30Interface()
        ).flatMap(Collection::stream).collect(Collectors.toList());

        String otherIssues = collectNonCompliantUrls(
                otherIssuesResponses,
                ""
        );


        // 取出大盘慢请求均值
        String averageSlowRequestRateInThePastWeek = TableUtils.getPercentageFormatDouble(result.getAverageSlowRequestRateInThePastWeek());
        // 取出月均值
        List<SlowRequestRateModel> gatewayAverageSlowRequestRate = result.getGatewayAverageSlowRequestRate();
        // gatewayAverageSlowRequestRate按照month倒序排列
        gatewayAverageSlowRequestRate.sort(Comparator.comparingInt(SlowRequestRateModel::getMonth).reversed());

        String slowRequestRateThisMonth = TableUtils.getPercentageFormatDouble(gatewayAverageSlowRequestRate.get(0).getSlowRequestRate());
        String monthlySummaryTemplate = "@所有人\n%d月(cl-gateway)网关慢请求率概况：\n--月均值：%s\n--本周（%s）大盘均值：%s\n";
        String monthlySummary = String.format(monthlySummaryTemplate, monthValue, slowRequestRateThisMonth, dateRange, averageSlowRequestRateInThePastWeek);
        setText(document, monthlySummary);
        setSecondLevelTitle(document, String.format("3.1 核心接口（关键路径）未达标 需重点关注：%s日\n", endDate.format(formatter)));
        setText(document, criticalIssues);
        setSecondLevelTitle(document, String.format("3.2 其他性能恶化的接口( %s 对比，99 线增加超 30ms，且环比增幅超 10%%)：\n", dateRange));
        setText(document, otherIssues);
        // 生成报告
        setText(document, "请以上接口负责人提供性能恶化的原因，并推进相关治理措施。\n本周数据明细详见 ：");
    }


    /**
     * 处理不达标的接口数据
     */
    private String collectNonCompliantUrls(List<UrlPerformanceResponse> urlList, String header) {
        StringJoiner joiner = new StringJoiner("\n", header + "\n", "\n");
        urlList.stream()
                .filter(url -> !url.getReachTarget())
                .forEach(url -> joiner.add(
                        String.format("【%s】%s\n 99线变化：%dms  @%s",
                                url.getPageName(), url.getUrl(), url.getP99Change(), url.getOwner())
                ));
        return joiner.toString();
    }

    private String collectNonCompliantCriticalLinkUrls(List<UrlPerformanceResponse> urlList, String header) {
        StringJoiner joiner = new StringJoiner("\n", header + "\n", "\n");
        urlList.stream()
                .filter(url -> !url.getReachTarget())
                .forEach(url -> joiner.add(
                        String.format("【%s】%s\n 99线：%dms   99线基线目标：%dms  @%s",
                                url.getPageName(), url.getUrl(), url.getThisWeekP99(), url.getP99Target(), url.getOwner())
                ));
        return joiner.toString();
    }

    /**
     * 统一保存 Word 文档
     */
    private void saveWordDocument(XWPFDocument document, String directoryPath) throws IOException {
        String fileName = URLEncoder.encode("performance.docx", StandardCharsets.UTF_8.toString());
        String filePath = directoryPath + "/" + fileName;

        try (FileOutputStream out = new FileOutputStream(filePath)) {
            document.write(out);
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
                return "请求量TOP30接口";
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
        drawCommonTable(document, responses, startDate, endDate, true);
    }

    private void drawOtherCoreBusinessInterfaceTable(XWPFDocument document,
                                                     List<UrlPerformanceResponse> responses,
                                                     LocalDate startDate,
                                                     LocalDate endDate) {


        drawCommonTable(document, responses, startDate, endDate, true);
    }

    private void drawQilinComponentInterfaceTable(XWPFDocument document,
                                                  List<UrlPerformanceResponse> responses,
                                                  LocalDate startDate,
                                                  LocalDate endDate) {


        drawCommonTable(document, responses, startDate, endDate, true);
    }

    private void drawFirstScreenTabTable(XWPFDocument document,
                                         List<UrlPerformanceResponse> responses,
                                         LocalDate startDate,
                                         LocalDate endDate) {
        drawCommonTable(document, responses, startDate, endDate, true);

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
                dateStrings[0] + "日90线",
                dateStrings[1] + "日90线",
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
        row.getCell(TableColumns.LAST_WEEK_P90).setText(String.valueOf(entity.getLastWeekP90()));
        row.getCell(TableColumns.THIS_WEEK_P90).setText(String.valueOf(entity.getThisWeekP90()));
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
        drawCommonTable(document, responses, startDate, endDate, true);
    }

    private void drawCriticalPathTable(XWPFDocument document,
                                       List<UrlPerformanceResponse> responses,
                                       LocalDate startDate,
                                       LocalDate endDate) {
        drawCommonTable(document, responses, startDate, endDate, true);
    }


    // 新增通用表格生成方法
    public <T> void drawTable(XWPFDocument document,
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
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();

        // 按换行符分割文本
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            run.setText(lines[i]);
            if (i < lines.length - 1) {
                run.addBreak(); // 添加换行符（同段落内换行）
                // 或用 run.addBreak(BreakType.TEXT_WRAPPING);
            }
        }
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