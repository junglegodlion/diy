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


    public void getGateWayPerformanceCurveChart(Integer year, @PastOrPresent LocalDate startDate, HttpServletResponse response) {
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=performance_chart.xlsx");
        // 获取该year年的"cl-gateway.tuhu.cn"的性能数据
        LocalDate date = LocalDate.of(year, 1, 1);
        String host = "cl-gateway.tuhu.cn";
        List<GateWayDailyPerformanceEntity> performanceByYear = gateWayDailyPerformanceMapper.getPerformanceByYear(host, date);

        // performanceByYear按照date排序
        performanceByYear.sort(Comparator.comparing(GateWayDailyPerformanceEntity::getDate));

        // 获取该年的99线
        List<P99Model> yearP99Models = getNewP99Models(performanceByYear);
        // 获取该月99线
        List<P99Model> monthP99Models = getMonthP99Models(performanceByYear, startDate);

        // 获取该年周维度平均99线
        List<P99Model> averageP99Models = PerformanceUtils.getAverageP99Models(yearP99Models);

        // 获取该月慢请求率
        List<SlowRequestRateModel> monthSlowRequestRateModels = getMonthSlowRequestRateModels(performanceByYear, startDate);

        // 慢请求率
        List<SlowRequestRateModel> yearSlowRequestRateModels = getSlowRequestRateModels(performanceByYear);

        // 周维度慢请求率
        List<SlowRequestRateModel> averageSlowRequestRateModels = PerformanceUtils.getAverageSlowRequestRateModels(yearSlowRequestRateModels);

        // 画图
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // 定义 Sheet 名称和数据列表
            String[] sheetNames = {"99线", "周维度99线", "慢请求率", "周维度慢请求率"};

            createP99ModelSheet(workbook, sheetNames[0], monthP99Models, "gateway 99线", "日期", "99线", "99线");
            createP99ModelSheet(workbook, sheetNames[1], averageP99Models, "gateway 99线-周维度", "日期", "99线", "99线");
            createSlowRequestRateModelSheet(workbook, sheetNames[2], monthSlowRequestRateModels, "gateway 慢请求率", "日期", "慢请求率", "慢请求率");
            createSlowRequestRateModelSheet(workbook, sheetNames[3], averageSlowRequestRateModels, "gateway 慢请求率-周维度", "日期", "慢请求率", "慢请求率");

            // 拼接完整的文件路径
            // 获取当天日期并格式化为 yyyy-MM-dd 格式
            LocalDate currentDate = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String formattedDate = currentDate.format(formatter);
            String fileName = URLEncoder.encode(formattedDate + "_chart.xlsx", StandardCharsets.UTF_8.toString());
            String directoryPath = System.getProperty("user.home") + "/Desktop/备份/c端网关接口性能统计/数据统计/输出/图表";
            String filePath = directoryPath + "/" + fileName;

            // 写入文件
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            } catch (IOException e) {
                log.error("ExportService#exportToExcel,出现异常！", e);
            }

            // 6. 保存文件
            workbook.write(response.getOutputStream());
        } catch (Exception e) {
            log.error("FileReadController#getCharts,出现异常！", e);
        }

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
