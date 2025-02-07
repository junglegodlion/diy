package com.jungo.diy.controller;

import com.jungo.diy.model.ExcelModel;
import com.jungo.diy.model.InterfacePerformanceModel;
import com.jungo.diy.model.SheetModel;
import com.jungo.diy.model.UrlPerformanceModel;
import com.jungo.diy.response.UrlPerformanceResponse;
import com.jungo.diy.service.ExportService;
import com.jungo.diy.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author lichuang3
 * @date 2025-02-06 12:51
 */
@RestController
public class FileReadController {

    @Autowired
    FileService fileService;

    @Autowired
    private ExportService exportService;

    @PostMapping("/upload")
    public void readFile(@RequestParam("file") MultipartFile file, HttpServletResponse response) throws IOException {
        String filename = file.getOriginalFilename();
        if (Objects.isNull(filename)) {
            throw new IllegalArgumentException("文件名为空");
        }

        ExcelModel data = null;
        if (filename.endsWith(".xlsx")) {
            data = readXlsxFile(file);
        } else {
            throw new IllegalArgumentException("不支持的文件类型");
        }

        // 上周的接口性能数据
        List<InterfacePerformanceModel> lastWeek = getInterfacePerformanceModels(data.getSheetModels().get(0), data.getSheetModels().get(1));
        // 本周的接口性能数据
        List<InterfacePerformanceModel> thisWeek = getInterfacePerformanceModels(data.getSheetModels().get(2), data.getSheetModels().get(3));
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
            if (criticalLink.contains(url)
                    || fiveGangJing.contains(url)
                    || firstScreenTab.contains(url)
                    || qilinComponentInterface.contains(url)
                    || otherCoreBusinessInterface.contains(url)) {

            } else {
                UrlPerformanceResponse urlPerformanceResponse = getUrlPerformanceResponse(url, urlPerformanceModelMap);
                accessVolumeTop30Interface.add(urlPerformanceResponse);
            }
        }

        exportService.exportToExcel(criticalLinkUrlPerformanceResponses, fiveGangJingUrlPerformanceResponses, firstScreenTabUrlPerformanceResponses, qilinComponentInterfaceUrlPerformanceResponses, otherCoreBusinessInterfaceUrlPerformanceResponses, accessVolumeTop30Interface, response);
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

    private List<InterfacePerformanceModel> getInterfacePerformanceModels(SheetModel requestsheetModel, SheetModel slowRequestCountSheetModel) {
        // 将slowRequestCountSheetModel转换成map
        Map<String, Integer> slowRequestCountMap = slowRequestCountSheetModel.getData().stream().collect(Collectors.toMap(x -> x.get(0) + x.get(1), x -> convertStringToInteger(x.get(2)), (x, y) -> x));

        // 请求情况
        List<InterfacePerformanceModel> interfacePerformanceModels = new ArrayList<>();
        for (List<String> datum : requestsheetModel.getData()) {
            InterfacePerformanceModel interfacePerformanceModel = new InterfacePerformanceModel();
            String token = datum.get(0) + datum.get(1);
            interfacePerformanceModel.setToken(token);
            interfacePerformanceModel.setHost(datum.get(0));
            interfacePerformanceModel.setUrl(datum.get(1));
            interfacePerformanceModel.setTotalRequestCount(convertStringToInteger(datum.get(2)));
            interfacePerformanceModel.setP99(convertStringToInteger(datum.get(4)));
            Integer slowRequestCount = slowRequestCountMap.get(token);
            if (Objects.isNull(slowRequestCount)) {
                slowRequestCount = 0;
            }
            interfacePerformanceModel.setSlowRequestCount(slowRequestCount);
            interfacePerformanceModels.add(interfacePerformanceModel);
        }

        return interfacePerformanceModels;
    }

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

    // 调用示例
    Integer result = convertStringToInteger("9.0"); // 返回 9

    private ExcelModel readXlsxFile(MultipartFile file) throws IOException {
        // XLSX 文件读取逻辑
        return fileService.readXlsxFile(file);
    }

    private List<List<String>> readCsvFile(MultipartFile file) throws IOException {
        // CSV 文件读取逻辑
        return fileService.readCsvFile(file);
    }
}
