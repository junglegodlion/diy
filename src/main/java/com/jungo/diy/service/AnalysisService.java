package com.jungo.diy.service;

import com.jungo.diy.entity.ApiDailyPerformanceEntity;
import com.jungo.diy.entity.GateWayDailyPerformanceEntity;
import com.jungo.diy.mapper.ApiDailyPerformanceMapper;
import com.jungo.diy.mapper.GateWayDailyPerformanceMapper;
import com.jungo.diy.model.InterfacePerformanceModel;
import com.jungo.diy.model.P99Model;
import com.jungo.diy.model.SlowRequestRateModel;
import com.jungo.diy.model.UrlPerformanceModel;
import com.jungo.diy.response.UrlPerformanceResponse;
import com.jungo.diy.util.PerformanceUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

    public String get99LineCurve(String url, LocalDate startDate, LocalDate endDate, HttpServletResponse response) {
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

    public void getCorePerformanceCompare(LocalDate startDate,
                                            LocalDate endDate,
                                            HttpServletResponse response) {

        // 获取startDate这天的所有接口性能数据
        List<ApiDailyPerformanceEntity> startDateApiDailyPerformanceEntities = apiDailyPerformanceMapper.findAllByDate(startDate);
        // 获取endDate这天的所有接口性能数据
        List<ApiDailyPerformanceEntity> endDateApiDailyPerformanceEntities = apiDailyPerformanceMapper.findAllByDate(endDate);
        // 上周的接口性能数据
        List<InterfacePerformanceModel> lastWeek = startDateApiDailyPerformanceEntities.stream().map(x -> {
            InterfacePerformanceModel newInterfacePerformanceModel = new InterfacePerformanceModel();
            newInterfacePerformanceModel.setToken(x.getHost() + x.getUrl());
            newInterfacePerformanceModel.setHost(x.getHost());
            newInterfacePerformanceModel.setUrl(x.getUrl());
            newInterfacePerformanceModel.setTotalRequestCount(x.getTotalRequestCount());
            newInterfacePerformanceModel.setP99(x.getP99());
            newInterfacePerformanceModel.setTotalRequestCount(x.getTotalRequestCount());
            newInterfacePerformanceModel.setSlowRequestCount(x.getSlowRequestCount());

            return newInterfacePerformanceModel;
        }).collect(Collectors.toList());
        // 本周的接口性能数据
        List<InterfacePerformanceModel> thisWeek = endDateApiDailyPerformanceEntities.stream().map(x -> {
            InterfacePerformanceModel newInterfacePerformanceModel = new InterfacePerformanceModel();
            newInterfacePerformanceModel.setToken(x.getHost() + x.getUrl());
            newInterfacePerformanceModel.setHost(x.getHost());
            newInterfacePerformanceModel.setUrl(x.getUrl());
            newInterfacePerformanceModel.setTotalRequestCount(x.getTotalRequestCount());
            newInterfacePerformanceModel.setP99(x.getP99());
            newInterfacePerformanceModel.setTotalRequestCount(x.getTotalRequestCount());
            newInterfacePerformanceModel.setSlowRequestCount(x.getSlowRequestCount());

            return newInterfacePerformanceModel;
        }).collect(Collectors.toList());

        // 将lastWeek转换成map
        Map<String, InterfacePerformanceModel> lastWeekMap = lastWeek.stream().collect(Collectors.toMap(InterfacePerformanceModel::getToken, x -> x, (x, y) -> x));
        // 将thisWeek转换成map
        Map<String, InterfacePerformanceModel> thisWeekMap = thisWeek.stream().collect(Collectors.toMap(InterfacePerformanceModel::getToken, x -> x, (x, y) -> x));
        // 组装UrlPerformanceModel对象
        List<UrlPerformanceModel> urlPerformanceModels = new ArrayList<>();
        for (Map.Entry<String, InterfacePerformanceModel> entry : thisWeekMap.entrySet()) {
            String token = entry.getKey();
            InterfacePerformanceModel thisWeekInterfacePerformanceModel = entry.getValue();
            InterfacePerformanceModel lastWeekInterfacePerformanceModel = lastWeekMap.get(token);
            if (Objects.nonNull(lastWeekInterfacePerformanceModel)) {
                UrlPerformanceModel urlPerformanceModel = new UrlPerformanceModel();
                urlPerformanceModel.setToken(token);
                urlPerformanceModel.setHost(thisWeekInterfacePerformanceModel.getHost());
                urlPerformanceModel.setUrl(thisWeekInterfacePerformanceModel.getUrl());
                urlPerformanceModel.setLastWeek(lastWeekInterfacePerformanceModel);
                urlPerformanceModel.setThisWeek(thisWeekInterfacePerformanceModel);
                urlPerformanceModels.add(urlPerformanceModel);
            }

        }

        // urlPerformanceModels转成map
        Map<String, UrlPerformanceModel> urlPerformanceModelMap = urlPerformanceModels.stream().collect(Collectors.toMap(UrlPerformanceModel::getUrl, x -> x, (x, y) -> x));

        // 关键链路
        List<String> criticalLink = new ArrayList<>();
        // 向列表中添加数据
        criticalLink.add("/cl-tire-site/tireListModule/getTireList");
        criticalLink.add("/cl-maint-api/maintMainline/getBasicMaintainData");
        criticalLink.add("/cl-maint-mainline/mainline/getDynamicData");
        criticalLink.add("/cl-oto-front-api/batteryList/getBatteryList");
        criticalLink.add("/mlp-product-search-api/module/search/pageList");
        criticalLink.add("/mlp-product-search-api/main/search/api/mainProduct");
        criticalLink.add("/ext-website-cl-beauty-api/channelPage/v4/getBeautyHomeShopListAndRecommendLabel");
        criticalLink.add("/cl-product-components/GoodsDetail/detailModuleInfo");
        criticalLink.add("/cl-tire-site/tireModule/getTireDetailModuleData");
        criticalLink.add("/cl-maint-mainline/productMainline/getMaintProductDetailInfo");
        criticalLink.add("/cl-ordering-aggregator/ordering/getOrderConfirmFloatLayerData");
        criticalLink.add("/cl-maint-order-create/order/getConfirmOrderData");

        List<UrlPerformanceResponse>  criticalLinkUrlPerformanceResponses = new ArrayList<>();
        for (String url : criticalLink) {
            if (urlPerformanceModelMap.containsKey(url)) {
                UrlPerformanceResponse urlPerformanceResponse = getUrlPerformanceResponse(url, urlPerformanceModelMap);
                criticalLinkUrlPerformanceResponses.add(urlPerformanceResponse);
            }
        }
        // 五大金刚
        List<String> fiveGangJing = new ArrayList<>();
        fiveGangJing.add("/cl-maint-api/apinew/GetBaoYangAppPackages");
        fiveGangJing.add("/cl-maint-api/apinew/getBasicMaintainData");
        fiveGangJing.add("/cl-tire-site/tireList/getCombineList");
        fiveGangJing.add("/cl-tire-site/tireList/getFilterItem");
        fiveGangJing.add("/ext-website-cl-beauty-api/channelPage/v4/getBeautyHomeModule");
        fiveGangJing.add("/ext-website-cl-beauty-api/channelPage/v4/getCategoryAndShopList");
        fiveGangJing.add("/ext-website-cl-beauty-api/channelPage/v4/getBeautyShopList");
        fiveGangJing.add("/ext-website-cl-beauty-api/beautyIndex/getCategoryAndShopListV2");
        fiveGangJing.add("/ext-website-cl-beauty-api/beautyIndex/getBeautyShopListV2");
        fiveGangJing.add("/cl-list-aggregator/channel/getChannelModuleInfo");
        fiveGangJing.add("/cl-repair-mainline/mainline/v4/getCategoryList");
        fiveGangJing.add("/cl-repair-mainline/mainline/v4/getDynamicData");
        List<UrlPerformanceResponse> fiveGangJingUrlPerformanceResponses = new ArrayList<>();
        for (String url : fiveGangJing) {
            if (urlPerformanceModelMap.containsKey(url)) {
                UrlPerformanceResponse urlPerformanceResponse = getUrlPerformanceResponse(url, urlPerformanceModelMap);
                fiveGangJingUrlPerformanceResponses.add(urlPerformanceResponse);
            }
        }
        // 首屏tab
        List<String> firstScreenTab = new ArrayList<>();
        firstScreenTab.add("/cl-homepage-service/homePage/getHomePageInfo");
        firstScreenTab.add("/cl-homepage-service/cmsService/getInterfaceData");
        firstScreenTab.add("/cl-homepage-service/tabBarService/getNewTabBars");
        firstScreenTab.add("/cl-homepage-service/homePage/speciallySaleRecommend");
        firstScreenTab.add("/cl-user-info-site/userVehicle/getEstimateMileage");
        firstScreenTab.add("/cl-user-info-site/maint-record/car-latest-condition");
        firstScreenTab.add("/cl-user-car-site/maint-record/mergeList");
        firstScreenTab.add("/cl-shop-api/shopTab/getModuleForC");
        firstScreenTab.add("/cl-shop-api/shopFilterItem/getShopFilterItemList");
        firstScreenTab.add("/cl-shop-api/shopList/getMainShopList");
        firstScreenTab.add("/cl-common-api/api/personalCenter/getNewQaMsgV2");
        firstScreenTab.add("/cl-common-api/api/personalCenter/getPersonalOrderInfo");
        firstScreenTab.add("/cl-common-api/api/personalCenter/getPersonalProductInfo");
        firstScreenTab.add("/cl-common-api/api/personalCenter/getCmsModuleList");
        firstScreenTab.add("/cl-common-api/api/personalCenter/getNewOrderInfo");
        firstScreenTab.add("/cl-common-api/api/personalCenter/getTopBanner");
        firstScreenTab.add("/cl-common-api/api/personalCenter/getAutoSuperConfig");
        List<UrlPerformanceResponse> firstScreenTabUrlPerformanceResponses = new ArrayList<>();
        for (String url : firstScreenTab) {
            if (urlPerformanceModelMap.containsKey(url)) {
                UrlPerformanceResponse urlPerformanceResponse = getUrlPerformanceResponse(url, urlPerformanceModelMap);
                firstScreenTabUrlPerformanceResponses.add(urlPerformanceResponse);
            }
        }

        // 麒麟组件接口
        List<String> qilinComponentInterface = new ArrayList<>();
        qilinComponentInterface.add("/cl-maint-api/activity/getProducts");
        qilinComponentInterface.add("/cl-maint-api/greatValueCard/getActivityCards");
        qilinComponentInterface.add("/cl-maint-api/activity/getSpikeDynamicPackages");
        qilinComponentInterface.add("/cl-tire-site/activityPage/getKylinActivityPageProductList");
        qilinComponentInterface.add("/ext-website-cl-beauty-api/beautyKylin/getShopList");
        qilinComponentInterface.add("/cl-car-product-api/Product/getChannelActivityComponent");
        qilinComponentInterface.add("/cl-car-product-api/Product/getActivityComponentDetail");
        qilinComponentInterface.add("/cl-oto-front-api/battery/BatteryActivityComponentDetail");
        qilinComponentInterface.add("/cl-shop-api/component/getKylinShopList");
        qilinComponentInterface.add("/cl-kael-agg-service/salePlan/getCombinedCommodityPool");
        qilinComponentInterface.add("/cl-common-api/api/memberPlus/getPlusCardChannelInfo");

        List<UrlPerformanceResponse> qilinComponentInterfaceUrlPerformanceResponses = new ArrayList<>();
        for (String url : qilinComponentInterface) {
            if (urlPerformanceModelMap.containsKey(url)) {
                UrlPerformanceResponse urlPerformanceResponse = getUrlPerformanceResponse(url, urlPerformanceModelMap);
                qilinComponentInterfaceUrlPerformanceResponses.add(urlPerformanceResponse);
            }
        }

        // 其他核心业务接口
        List<String> otherCoreBusinessInterface = new ArrayList<>();
        otherCoreBusinessInterface.add("/ext-website-cl-beauty-api/beautyProduct/getBeautyProductDetailV2");
        otherCoreBusinessInterface.add("/cl-ordering-aggregator/ordering/getOrderConfirmData");
        otherCoreBusinessInterface.add("/ext-website-cl-beauty-api/order/getOrderCheckInfo");
        otherCoreBusinessInterface.add("/cl-ordering-aggregator/ordering/createOrder");
        otherCoreBusinessInterface.add("/cl-maint-order-create/order/createorder");

        List<UrlPerformanceResponse> otherCoreBusinessInterfaceUrlPerformanceResponses = new ArrayList<>();
        for (String url : otherCoreBusinessInterface) {
            if (urlPerformanceModelMap.containsKey(url)) {
                UrlPerformanceResponse urlPerformanceResponse = getUrlPerformanceResponse(url, urlPerformanceModelMap);
                otherCoreBusinessInterfaceUrlPerformanceResponses.add(urlPerformanceResponse);
            }
        }
        // 访问量top30接口
        List<UrlPerformanceResponse> accessVolumeTop30Interface = new ArrayList<>();
        // 首先将urlPerformanceModels排除host为"mkt-gateway.tuhu.cn"的对象，然后按照thisWeek.totalRequestCount逆序排序，最后取前30个url
        List<UrlPerformanceModel> sortUrlPerformanceModels = urlPerformanceModelMap.values().stream().filter(urlPerformanceModel -> !"mkt-gateway.tuhu.cn".equals(urlPerformanceModel.getHost())).sorted((o1, o2) -> o2.getThisWeek().getTotalRequestCount() - o1.getThisWeek().getTotalRequestCount()).collect(Collectors.toList());

        for (int i = 0; i < 30; i++) {
            String url = sortUrlPerformanceModels.get(i).getUrl();
            if (!criticalLink.contains(url)
                    && !fiveGangJing.contains(url)
                    && !firstScreenTab.contains(url)
                    && !qilinComponentInterface.contains(url)
                    && !otherCoreBusinessInterface.contains(url)) {
                UrlPerformanceResponse urlPerformanceResponse = getUrlPerformanceResponse(url, urlPerformanceModelMap);
                accessVolumeTop30Interface.add(urlPerformanceResponse);
            }
        }

        try {
            exportService.exportToExcel(criticalLinkUrlPerformanceResponses, fiveGangJingUrlPerformanceResponses, firstScreenTabUrlPerformanceResponses, qilinComponentInterfaceUrlPerformanceResponses, otherCoreBusinessInterfaceUrlPerformanceResponses, accessVolumeTop30Interface, response);
        } catch (IOException e) {
            log.error("AnalysisService#getCorePerformanceCompare,出现异常！", e);
        }
    }

    private static UrlPerformanceResponse getUrlPerformanceResponse(String url, Map<String, UrlPerformanceModel> urlPerformanceModelMap) {
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
}
