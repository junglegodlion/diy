package com.jungo.diy.service;

import com.jungo.diy.config.RequestContext;
import com.jungo.diy.entity.ApiDailyPerformanceEntity;
import com.jungo.diy.entity.GateWayDailyPerformanceEntity;
import com.jungo.diy.enums.InterfaceTypeEnum;
import com.jungo.diy.mapper.ApiDailyPerformanceMapper;
import com.jungo.diy.mapper.GateWayDailyPerformanceMapper;
import com.jungo.diy.model.P99Model;
import com.jungo.diy.model.SlowRequestRateModel;
import com.jungo.diy.model.UrlPerformanceModel;
import com.jungo.diy.repository.PerformanceRepository;
import com.jungo.diy.response.UrlPerformanceResponse;
import com.jungo.diy.util.PerformanceUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.PastOrPresent;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jungo.diy.controller.FileReadController.*;

/**
 * @author lichuang3
 * @date 2025-02-19 10:21
 */
@Service
@Slf4j
public class AnalysisService {
    @Autowired
    private ApiDailyPerformanceMapper apiDailyPerformanceMapper;
    @Autowired
    private ExportService exportService;
    @Autowired
    private GateWayDailyPerformanceMapper gateWayDailyPerformanceMapper;
    @Autowired
    private PerformanceRepository performanceRepository;

    public String get99LineCurve(String url, HttpServletResponse response) {
        LocalDate startDate = RequestContext.getAs("startDate", LocalDate.class);
        LocalDate endDate = RequestContext.getAs("endDate", LocalDate.class);
        List<ApiDailyPerformanceEntity> apiDailyPerformanceEntities = apiDailyPerformanceMapper.findUrl99Line(url, startDate, endDate);
        // apiDailyPerformanceEntities按照日期排序
        apiDailyPerformanceEntities.sort(Comparator.comparing(ApiDailyPerformanceEntity::getDate));

        List<P99Model> p99Models = getP99Models(apiDailyPerformanceEntities);
        // 画图
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            createP99ModelSheet(workbook, "99线变化率", p99Models, "gateway 99线", "日期", "99线", "99线");
            // 6. 保存文件
            workbook.write(response.getOutputStream());
        } catch (Exception e) {
            log.error("AnalysisService#get99LineCurve,出现异常！", e);
        }
        return "success";
    }

    private List<P99Model> getP99Models(List<ApiDailyPerformanceEntity> apiDailyPerformanceEntities) {
        List<P99Model> p99Models = new ArrayList<>();
        for (ApiDailyPerformanceEntity apiDailyPerformanceEntity : apiDailyPerformanceEntities) {
            P99Model p99Model = new P99Model();
            Date date = apiDailyPerformanceEntity.getDate();
            // 将 Date 对象转换为 LocalDate 对象
            Instant instant = date.toInstant();
            LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
            // 定义日期格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            // 将 LocalDate 对象转换为字符串
            String dateString = localDate.format(formatter);
            p99Model.setDate(dateString);
            p99Model.setP99(apiDailyPerformanceEntity.getP99());
            p99Models.add(p99Model);
        }
        return p99Models;
    }

    private List<P99Model> getNewP99Models(List<GateWayDailyPerformanceEntity> gateWayDailyPerformanceEntities) {
        List<P99Model> p99Models = new ArrayList<>();
        for (GateWayDailyPerformanceEntity gateWayDailyPerformanceEntity : gateWayDailyPerformanceEntities) {
            P99Model p99Model = new P99Model();
            Date date = gateWayDailyPerformanceEntity.getDate();
            // 将 Date 对象转换为 LocalDate 对象
            Instant instant = date.toInstant();
            LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
            // 定义日期格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            // 将 LocalDate 对象转换为字符串
            String dateString = localDate.format(formatter);
            p99Model.setDate(dateString);
            p99Model.setPeriod(gateWayDailyPerformanceEntity.getWeekNumber());
            p99Model.setP99(gateWayDailyPerformanceEntity.getP99());
            p99Models.add(p99Model);
        }
        return p99Models;
    }

    public String getCorePerformanceCompare(LocalDate startDate,
                                            LocalDate endDate,
                                            HttpServletResponse response) {

        Map<String, UrlPerformanceModel> urlPerformanceModelMap = performanceRepository.getUrlPerformanceModelMap(startDate, endDate);
        // 关键链路
        List<UrlPerformanceResponse> criticalLinkUrlPerformanceResponses = performanceRepository.getUrlPerformanceResponses(InterfaceTypeEnum.CRITICAL_LINK.getCode(), urlPerformanceModelMap);

        // 五大金刚
        List<UrlPerformanceResponse> fiveGangJingUrlPerformanceResponses = performanceRepository.getUrlPerformanceResponses(InterfaceTypeEnum.FIVE_GANG_JING.getCode(), urlPerformanceModelMap);

        // 首屏tab
        List<UrlPerformanceResponse> firstScreenTabUrlPerformanceResponses = performanceRepository.getUrlPerformanceResponses(InterfaceTypeEnum.FIRST_SCREEN_TAB.getCode(), urlPerformanceModelMap);

        // 麒麟组件接口
        List<UrlPerformanceResponse> qilinComponentInterfaceUrlPerformanceResponses = performanceRepository.getUrlPerformanceResponses(InterfaceTypeEnum.QILIN_COMPONENT_INTERFACE.getCode(), urlPerformanceModelMap);

        // 其他核心业务接口
        List<UrlPerformanceResponse> otherCoreBusinessInterfaceUrlPerformanceResponses = performanceRepository.getUrlPerformanceResponses(InterfaceTypeEnum.OTHER_CORE_BUSINESS_INTERFACE.getCode(), urlPerformanceModelMap);

        // 访问量top30接口
        // 首先将urlPerformanceModels排除host为"mkt-gateway.tuhu.cn"的对象，然后按照thisWeek.totalRequestCount逆序排序，最后取前30个url
        List<UrlPerformanceModel> sortUrlPerformanceModels = urlPerformanceModelMap.values().stream()
                .filter(urlPerformanceModel -> !"mkt-gateway.tuhu.cn".equals(urlPerformanceModel.getHost()))
                .sorted((o1, o2) -> o2.getThisWeek().getTotalRequestCount() - o1.getThisWeek().getTotalRequestCount()).collect(Collectors.toList());
        List<UrlPerformanceResponse> accessVolumeTop30Interface = sortUrlPerformanceModels.stream()
                .limit(30)
                .filter(urlPerformanceModel -> {
                    String url = urlPerformanceModel.getUrl();
                    return Stream.of(criticalLinkUrlPerformanceResponses, fiveGangJingUrlPerformanceResponses,
                                    firstScreenTabUrlPerformanceResponses, qilinComponentInterfaceUrlPerformanceResponses,
                                    otherCoreBusinessInterfaceUrlPerformanceResponses)
                            .noneMatch(list -> coreInterfaceContains(list, url));
                })
                .map(urlPerformanceModel -> getUrlPerformanceResponse(urlPerformanceModel.getUrl(), urlPerformanceModelMap))
                .collect(Collectors.toList());

        List<UrlPerformanceResponse>[] dataLists = new List[]{
                criticalLinkUrlPerformanceResponses,
                fiveGangJingUrlPerformanceResponses,
                firstScreenTabUrlPerformanceResponses,
                qilinComponentInterfaceUrlPerformanceResponses,
                otherCoreBusinessInterfaceUrlPerformanceResponses,
                accessVolumeTop30Interface
        };

        try {
            exportService.exportToExcel(dataLists, response);
        } catch (IOException e) {
            log.error("AnalysisService#getCorePerformanceCompare,出现异常！", e);
        }

        // 返回未达标的接口：格式如下：【轮胎列表页】/cl-tire-site/tireListModule/getTireList 99线：513ms 99线基线目标：450ms  @平会
        return getUnReachTargetUrl(dataLists);
    }

    private String getUnReachTargetUrl(List<UrlPerformanceResponse>[] dataLists) {
        StringBuilder str = new StringBuilder("未达标接口有：\n");
        List<UrlPerformanceResponse> dataList = dataLists[0];
        // 取出dataList不达标的数据
        for (UrlPerformanceResponse urlPerformanceResponse : dataList) {
            if (!urlPerformanceResponse.getReachTarget()) {
                str.append("【").append(urlPerformanceResponse.getPageName()).append("】").append(urlPerformanceResponse.getUrl()).append(" 99线：").append(urlPerformanceResponse.getThisWeekP99()).append("ms 99线基线目标：").append(urlPerformanceResponse.getP99Target()).append("ms  @").append(urlPerformanceResponse.getOwner()).append("\n");
            }
            // jungo TODO 2025/2/27:性能不满足要去30 10%
        }

        for (int i = 1; i < dataLists.length; i++) {
            List<UrlPerformanceResponse> performanceResponses = dataLists[i];
            // 取出dataList不达标的数据【轮胎列表主接口】/cl-tire-site/tireList/getCombineList 99线变化：+94ms  @平会
            for (UrlPerformanceResponse urlPerformanceResponse : performanceResponses) {
                if (!urlPerformanceResponse.getReachTarget()) {
                    str.append("【").append(urlPerformanceResponse.getPageName()).append("】").append(urlPerformanceResponse.getUrl()).append(" 99线变化：").append(urlPerformanceResponse.getP99Change()).append("ms  @").append(urlPerformanceResponse.getOwner()).append("\n");
                }
            }

        }
        String string = str.toString();
        System.out.println(string);
        return str.toString();
    }

    private static boolean coreInterfaceContains(List<UrlPerformanceResponse> urlPerformanceResponses, String url) {
        return urlPerformanceResponses.stream().anyMatch(coreInterfaceConfigEntity -> url.equals(coreInterfaceConfigEntity.getUrl()));
    }

    private static UrlPerformanceResponse getUrlPerformanceResponse(String url,
                                                                    Map<String, UrlPerformanceModel> urlPerformanceModelMap) {
        UrlPerformanceModel urlPerformanceModel = urlPerformanceModelMap.get(url);
        UrlPerformanceResponse urlPerformanceResponse = new UrlPerformanceResponse();
        urlPerformanceResponse.setHost(urlPerformanceModel.getHost());
        urlPerformanceResponse.setUrl(urlPerformanceModel.getUrl());
        urlPerformanceResponse.setLastWeekP99(urlPerformanceModel.getLastWeek().getP99());
        urlPerformanceResponse.setThisWeekP99(urlPerformanceModel.getThisWeek().getP99());
        urlPerformanceResponse.setLastWeekTotalRequestCount(urlPerformanceModel.getLastWeek().getTotalRequestCount());
        urlPerformanceResponse.setThisWeekTotalRequestCount(urlPerformanceModel.getThisWeek().getTotalRequestCount());
        urlPerformanceResponse.setLastWeekSlowRequestRate(urlPerformanceModel.getLastWeek().getSlowRequestRate());
        urlPerformanceResponse.setThisWeekSlowRequestRate(urlPerformanceModel.getThisWeek().getSlowRequestRate());
        urlPerformanceResponse.setP99Change(urlPerformanceModel.getP99Change());
        urlPerformanceResponse.setP99ChangeRate(urlPerformanceModel.getP99ChangeRate());
        // 99线是否达到目标值
        urlPerformanceResponse.setReachTarget(!(urlPerformanceModel.getP99ChangeRate() >= 0.1) || urlPerformanceModel.getP99Change() < 30);
        return urlPerformanceResponse;
    }

    /**
     * 生成网关性能曲线图表并导出为Excel文件
     * 功能：根据指定年份和起始日期，获取网关性能数据，计算各项指标（如99线、慢请求率等），生成多个工作表并输出到响应流和本地文件
     *
     * @param year      指定的年份，用于获取该年的性能数据
     * @param startDate 起始日期（过去或当前日期），用于计算该月的性能指标
     * @param response  HttpServletResponse对象，用于设置响应头并将Excel文件写入输出流
     */
    public void getGateWayPerformanceCurveChart(Integer year, @PastOrPresent LocalDate startDate, HttpServletResponse response) {
        // 设置响应头
        /* 配置HTTP响应头为Excel文件格式，并指定下载文件名 */
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=performance_chart.xlsx");
        /* 获取指定年份网关性能数据并排序 */
        LocalDate date = LocalDate.of(year, 1, 1);
        String host = "cl-gateway.tuhu.cn";
        List<GateWayDailyPerformanceEntity> performanceByYear = gateWayDailyPerformanceMapper.getPerformanceByYear(host, date);
        // performanceByYear按照date排序
        performanceByYear.sort(Comparator.comparing(GateWayDailyPerformanceEntity::getDate));

        /* 获取指定年份网关性能数据并排序 */
        // 获取该年的99线
        List<P99Model> yearP99Models = getNewP99Models(performanceByYear);
        // 获取该月99线
        List<P99Model> monthP99Models = getMonthP99Models(performanceByYear, startDate);
        // 获取该年周维度平均99线
        List<P99Model> averageP99Models = PerformanceUtils.getAverageP99Models(yearP99Models);

        /* 计算不同时间维度的慢请求率 */
        // 获取该月慢请求率
        List<SlowRequestRateModel> monthSlowRequestRateModels = getMonthSlowRequestRateModels(performanceByYear, startDate);
        // 慢请求率
        List<SlowRequestRateModel> yearSlowRequestRateModels = getSlowRequestRateModels(performanceByYear);
        // 周维度慢请求率
        List<SlowRequestRateModel> averageSlowRequestRateModels = PerformanceUtils.getAverageSlowRequestRateModels(yearSlowRequestRateModels);

        /* 创建Excel工作簿并生成多个数据表 */
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // 定义 Sheet 名称和数据列表
            String[] sheetNames = {"99线", "周维度99线", "慢请求率", "周维度慢请求率", "平均慢请求率"};

            /* 生成包含不同指标的工作表 */
            createP99ModelSheet(workbook, sheetNames[0], monthP99Models, "gateway 99线", "日期", "99线", "99线");
            createP99ModelSheet(workbook, sheetNames[1], averageP99Models, "gateway 99线-周维度", "日期", "99线", "99线");
            createSlowRequestRateModelSheet(workbook, sheetNames[2], monthSlowRequestRateModels, "gateway 慢请求率", "日期", "慢请求率", "慢请求率");
            createSlowRequestRateModelSheet(workbook, sheetNames[3], averageSlowRequestRateModels, "gateway 慢请求率-周维度", "日期", "慢请求率", "慢请求率");
            createAverageRowsModelSheet(workbook, sheetNames[4], performanceByYear);

            /* 生成文件路径并保存到本地 */
            LocalDate currentDate = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String formattedDate = currentDate.format(formatter);
            String fileName = URLEncoder.encode(formattedDate + "_chart.xlsx", StandardCharsets.UTF_8.toString());
            String directoryPath = System.getProperty("user.home") + "/Desktop/备份/c端网关接口性能统计/数据统计/输出/图表";
            String filePath = directoryPath + "/" + fileName;

            /* 执行文件写入操作 */
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            } catch (IOException e) {
                log.error("ExportService#exportToExcel,出现异常！", e);
            }

            /* 将工作簿写入HTTP响应输出流 */
            workbook.write(response.getOutputStream());
        } catch (Exception e) {
            log.error("FileReadController#getCharts,出现异常！", e);
        }

    }

    private void createAverageRowsModelSheet(XSSFWorkbook workbook,
                                             String sheetName,
                                             List<GateWayDailyPerformanceEntity> performanceByYear) {

        // 99线
        // 创建工作表
        XSSFSheet sheet = workbook.createSheet(sheetName);
        // 写入数据
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("host");
        headerRow.createCell(1).setCellValue("日期");
        headerRow.createCell(2).setCellValue("999线");
        headerRow.createCell(3).setCellValue("99线");
        headerRow.createCell(4).setCellValue("90线");
        headerRow.createCell(5).setCellValue("75线");
        headerRow.createCell(6).setCellValue("50线");
        headerRow.createCell(7).setCellValue("总请求数");
        headerRow.createCell(8).setCellValue("慢请求数");
        headerRow.createCell(9).setCellValue("慢请求率");

        headerRow.createCell(12).setCellValue("月");
        headerRow.createCell(13).setCellValue("月维度平均慢请求率");

        headerRow.createCell(15).setCellValue("最近一周平均慢请求率");



        // 创建百分比格式
        DataFormat dataFormat = workbook.createDataFormat();
        short percentageFormat = dataFormat.getFormat("0.00%");
        CellStyle percentageCellStyle = workbook.createCellStyle();
        percentageCellStyle.setDataFormat(percentageFormat);

        for (int i = 0; i < performanceByYear.size(); i++) {
            Row row = sheet.createRow(i + 1);
            GateWayDailyPerformanceEntity gateWayDailyPerformanceEntity = performanceByYear.get(i);
            row.createCell(0).setCellValue(gateWayDailyPerformanceEntity.getHost());
            Date date = gateWayDailyPerformanceEntity.getDate();
            Instant instant = date.toInstant();
            LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
            // 定义日期格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            // 将 LocalDate 对象转换为字符串
            String dateString = localDate.format(formatter);
            row.createCell(1).setCellValue(dateString);
            row.createCell(2).setCellValue(gateWayDailyPerformanceEntity.getP999());
            row.createCell(3).setCellValue(gateWayDailyPerformanceEntity.getP99());
            row.createCell(4).setCellValue(gateWayDailyPerformanceEntity.getP90());
            row.createCell(5).setCellValue(gateWayDailyPerformanceEntity.getP75());
            row.createCell(6).setCellValue(gateWayDailyPerformanceEntity.getP50());
            row.createCell(7).setCellValue(gateWayDailyPerformanceEntity.getTotalRequestCount());
            row.createCell(8).setCellValue(gateWayDailyPerformanceEntity.getSlowRequestCount());
            Cell cell = row.createCell(9);
            cell.setCellValue(gateWayDailyPerformanceEntity.getSlowRequestRate());
            cell.setCellStyle(percentageCellStyle);
        }

        // 计算月慢请求率平均值
        // 首先将performanceByYear的date字段按照"yyyy-MM"形式进行分组
        Map<String, List<GateWayDailyPerformanceEntity>> groupedByMonth = performanceByYear.stream()
                .collect(Collectors.groupingBy(entity -> {
                    LocalDate localDate = entity.getDate().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    return localDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                }));

        // 求取groupedByMonth value的平均值
        Map<String, Double> averageSlowRequestRateMap = new HashMap<>();
        groupedByMonth.forEach((key, value) -> {
            // 计算平均值
            double average = value.stream()
                    .mapToDouble(GateWayDailyPerformanceEntity::getSlowRequestRate)
                    .average()
                    .orElse(0.0);
            averageSlowRequestRateMap.put(key, average);
        });
        // 将averageSlowRequestRateMap中的key转化成list
        List<String> keys = new ArrayList<>(averageSlowRequestRateMap.keySet());

        for (int i = 0; i < keys.size(); i++) {
            Row row = sheet.getRow(i + 1);
            int finalI = i;
            Map.Entry<String, Double> entry = averageSlowRequestRateMap.entrySet().stream()
                    .filter(e -> e.getKey().equals(keys.get(finalI)))
                    .findFirst()
                    .orElse(null);
            if (entry != null) {
                row.createCell(12).setCellValue(entry.getKey());
                Cell cell = row.createCell(13);
                cell.setCellValue(entry.getValue());
                cell.setCellStyle(percentageCellStyle);
            }
        }

        // 取performanceByYear最后7个对象
        List<GateWayDailyPerformanceEntity> last7Days = performanceByYear.subList(performanceByYear.size() - 7, performanceByYear.size());
        // 求last7Days slowRequestRate的平均值
        double average = last7Days.stream()
                .mapToDouble(GateWayDailyPerformanceEntity::getSlowRequestRate)
                .average()
                .orElse(0.0);
        Row row = sheet.getRow(1);
        Cell cell = row.createCell(15);
        cell.setCellValue(average);
        cell.setCellStyle(percentageCellStyle);
    }

    private List<SlowRequestRateModel> getMonthSlowRequestRateModels(List<GateWayDailyPerformanceEntity> performanceByYear,
                                                                     @PastOrPresent LocalDate startDate) {
        // 过滤出>=startDate的数据
        List<GateWayDailyPerformanceEntity> collect = performanceByYear.stream()
                .filter(entity -> {
                    LocalDate localDate = entity.getDate().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

                    return startDate.isEqual(localDate) || startDate.isBefore(localDate);
                })
                .collect(Collectors.toList());

        return getSlowRequestRateModels(collect);
    }

    private List<P99Model> getMonthP99Models(List<GateWayDailyPerformanceEntity> performanceByYear, @PastOrPresent LocalDate startDate) {
        // 过滤出>=startDate的数据
        List<GateWayDailyPerformanceEntity> collect = performanceByYear.stream()
                .filter(entity -> {
                    LocalDate localDate = entity.getDate().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

                    return startDate.isEqual(localDate) || startDate.isBefore(localDate);
                })
                .collect(Collectors.toList());
        return getNewP99Models(collect);
    }

    private List<SlowRequestRateModel> getSlowRequestRateModels(List<GateWayDailyPerformanceEntity> performanceByYear) {
        List<SlowRequestRateModel> result = new ArrayList<>();
        for (GateWayDailyPerformanceEntity performance : performanceByYear) {
            SlowRequestRateModel slowRequestRateModel = new SlowRequestRateModel();
            Date date = performance.getDate();
            // 将 Date 对象转换为 LocalDate 对象
            Instant instant = date.toInstant();
            LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
            // 定义日期格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            // 将 LocalDate 对象转换为字符串
            String dateString = localDate.format(formatter);
            slowRequestRateModel.setDate(dateString);
            slowRequestRateModel.setPeriod(performance.getWeekNumber());
            slowRequestRateModel.setSlowRequestRate(performance.getSlowRequestRate());
            result.add(slowRequestRateModel);
        }
        return result;
    }

    public String batchGet99LineCurve(String[] urls, @PastOrPresent LocalDate startDate, LocalDate endDate, HttpServletResponse response) {
        Map<String, List<P99Model>> urlMap = new HashMap<>();
        for (String url : urls) {
            List<ApiDailyPerformanceEntity> apiDailyPerformanceEntities = apiDailyPerformanceMapper.findUrl99Line(url, startDate, endDate);
            // apiDailyPerformanceEntities按照日期排序
            apiDailyPerformanceEntities.sort(Comparator.comparing(ApiDailyPerformanceEntity::getDate));
            List<P99Model> p99Models = getP99Models(apiDailyPerformanceEntities);
            urlMap.put(url, p99Models);
        }

        // 画图
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // 定义 Sheet 名称和数据列表
            // 创建多个 Sheet 并写入数据
            for (String url : urls) {
                createP99Sheet(workbook, url, urlMap.get(url));
            }
            // 6. 保存文件
            workbook.write(response.getOutputStream());
        } catch (Exception e) {
            log.error("AnalysisService#get99LineCurve,出现异常！", e);
        }
        return "success";
    }

    private static void createP99Sheet(XSSFWorkbook workbook, String url, List<P99Model> p99Models) {
        // 99线
        // 创建工作表
        // 将url按照"/"分割，取最后一个元素作为sheetName
        String sheetName = url.substring(url.lastIndexOf("/") + 1);
        XSSFSheet sheet = workbook.createSheet(sheetName);
        // 写入数据
        createP99ModelsData(sheet, p99Models);
        // 3. 创建绘图对象
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 3, 5, 13, 20);
        int chartWidthCols = (int) Math.ceil((sheet.getLastRowNum() - 1) * 0.5);
        int endCol = anchor.getCol1() + chartWidthCols;
        anchor.setCol2(anchor.getCol1() + endCol);
        // 4. 创建图表对象
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("gateway 99线");
        chart.setTitleOverlay(false);
        // 5. 配置图表数据
        configurePerformanceLineChartData(chart, sheet, "日期",  "99线", "99线");
    }

    public String batchGetSlowRequestRateCurve(String[] urls,
                                               @PastOrPresent LocalDate startDate,
                                               LocalDate endDate,
                                               HttpServletResponse response) {
        Map<String, List<SlowRequestRateModel>> urlMap = new HashMap<>();
        for (String url : urls) {
            List<ApiDailyPerformanceEntity> apiDailyPerformanceEntities = apiDailyPerformanceMapper.getSlowRequestRate(url, startDate, endDate);
            // apiDailyPerformanceEntities按照日期排序
            apiDailyPerformanceEntities.sort(Comparator.comparing(ApiDailyPerformanceEntity::getDate));
            List<SlowRequestRateModel> slowRequestRateModels = getSlowRequestRateModelsNew(apiDailyPerformanceEntities);
            urlMap.put(url, slowRequestRateModels);
        }

        // 画图
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // 定义 Sheet 名称和数据列表
            // 创建多个 Sheet 并写入数据
            for (String url : urls) {
                createSlowRequestRateSheet(workbook, url, urlMap.get(url));
            }
            // 6. 保存文件
            workbook.write(response.getOutputStream());
        } catch (Exception e) {
            log.error("AnalysisService#get99LineCurve,出现异常！", e);
        }
        return "success";
    }

    private void createSlowRequestRateSheet(XSSFWorkbook workbook,
                                            String url,
                                            List<SlowRequestRateModel> slowRequestRateModels) {
        // 创建工作表
        // 将url按照"/"分割，取最后一个元素作为sheetName
        String sheetName = url.substring(url.lastIndexOf("/") + 1);
        XSSFSheet sheet = workbook.createSheet(sheetName);
        // 写入数据
        createSlowRequestRateModelsData(sheet, slowRequestRateModels);
        // 3. 创建绘图对象
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 3, 5, 13, 20);
        int chartWidthCols = (int) Math.ceil((sheet.getLastRowNum() - 1) * 0.5);
        int endCol = anchor.getCol1() + chartWidthCols;
        anchor.setCol2(anchor.getCol1() + endCol);
        // 4. 创建图表对象
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("gateway 慢请求率");
        chart.setTitleOverlay(false);
        // 5. 配置图表数据
        // 性能折线图
        configurePerformanceLineChartData(chart, sheet, "日期",  "慢请求率", "慢请求率");
    }

    private List<SlowRequestRateModel> getSlowRequestRateModelsNew(List<ApiDailyPerformanceEntity> apiDailyPerformanceEntities) {
        List<SlowRequestRateModel> slowRequestRateModels = new ArrayList<>();
        for (ApiDailyPerformanceEntity apiDailyPerformanceEntity : apiDailyPerformanceEntities) {
            SlowRequestRateModel slowRequestRateModel = new SlowRequestRateModel();
            Date date = apiDailyPerformanceEntity.getDate();
            // 将 Date 对象转换为 LocalDate 对象
            Instant instant = date.toInstant();
            LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
            // 定义日期格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            // 将 LocalDate 对象转换为字符串
            String dateString = localDate.format(formatter);
            slowRequestRateModel.setDate(dateString);
            double totalRequests = apiDailyPerformanceEntity.getTotalRequestCount();
            double slowRequestRate = totalRequests > 0
                    ? (double) apiDailyPerformanceEntity.getSlowRequestCount() / totalRequests
                    : 0.0;
            slowRequestRateModel.setSlowRequestRate(slowRequestRate);
            slowRequestRateModels.add(slowRequestRateModel);
        }
        return slowRequestRateModels;
    }
}
