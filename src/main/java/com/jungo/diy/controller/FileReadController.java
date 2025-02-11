package com.jungo.diy.controller;

import com.jungo.diy.model.*;
import com.jungo.diy.response.UrlPerformanceResponse;
import com.jungo.diy.service.ExportService;
import com.jungo.diy.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.PresetColor;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDLbls;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

import static com.jungo.diy.util.ExcelChartGenerator.*;


/**
 * @author lichuang3
 * @date 2025-02-06 12:51
 */
@RestController
@Slf4j
public class FileReadController {

    @Autowired
    FileService fileService;

    @Autowired
    private ExportService exportService;

    @PostMapping("/upload/getPerformance")
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


    @PostMapping("/upload/getCharts")
    public void getCharts(@RequestParam("file") MultipartFile file, HttpServletResponse response) throws IOException {
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=performance_chart.xlsx");

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

        // 99线
        List<P99Model> p99Models = getP99Models(data);
        // 周维度99线
        List<P99Model> averageP99Models = getAverageP99Models(p99Models);
        // 慢请求率
        List<SlowRequestRateModel> slowRequestRateModels = getSlowRequestRateModels(data);
        // 周维度慢请求率
        List<SlowRequestRateModel> averageSlowRequestRateModels = getAverageSlowRequestRateModels(slowRequestRateModels);
        // 画图
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // 定义 Sheet 名称和数据列表
            String[] sheetNames = {"99线", "周维度99线", "慢请求率", "周维度慢请求率"};

            // 99线
            // 创建工作表
            XSSFSheet sheet = workbook.createSheet(sheetNames[0]);
            // 写入数据
            createP99ModelsData(sheet, p99Models);
            // 3. 创建绘图对象
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 3, 5, 13, 20);
            // 4. 创建图表对象
            XSSFChart chart = drawing.createChart(anchor);
            chart.setTitleText("gateway 99线");
            chart.setTitleOverlay(false);
            // 5. 配置图表数据
            configureP99ModelsChartData(chart, sheet);
            // 6. 保存文件
            workbook.write(response.getOutputStream());
        } catch (Exception e) {
            log.error("FileReadController#getCharts,出现异常！", e);
        }
    }

    private void configureP99ModelsChartData(XSSFChart chart, XSSFSheet sheet) {
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
        XDDFChartData data = chart.createData(
                ChartTypes.LINE,
                getChartAxis(chart, "日期"),
                getValueAxis(chart, "99线")
        );

        // 4. 添加数据系列
        XDDFChartData.Series series = data.addSeries(categories, values);
        series.setTitle("99线", null);
        setLineStyle(series, PresetColor.YELLOW);

        // 5. 绘制图表
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

    private void createP99ModelsData(XSSFSheet sheet, List<P99Model> p99Models) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("日期");
        headerRow.createCell(1).setCellValue("99线");

        // 填充数据行
        List<String> dates = p99Models.stream().map(P99Model::getDate).collect(Collectors.toList());
        List<Integer> p99Values = p99Models.stream().map(P99Model::getP99).collect(Collectors.toList());

        for (int i = 0; i < dates.size(); i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(dates.get(i));
            row.createCell(1).setCellValue(p99Values.get(i));
        }

    }

    private List<SlowRequestRateModel> getAverageSlowRequestRateModels(List<SlowRequestRateModel> slowRequestRateModels) {
        // 将slowRequestRateModels按照周数分组，计算平均值
        Map<Integer, List<SlowRequestRateModel>> groupedSlowRequestRateModels = slowRequestRateModels.stream().collect(Collectors.groupingBy(SlowRequestRateModel::getPeriod));
        List<SlowRequestRateModel> averageSlowRequestRateModels = new ArrayList<>();
        for (Map.Entry<Integer, List<SlowRequestRateModel>> entry : groupedSlowRequestRateModels.entrySet()) {
            List<SlowRequestRateModel> slowRequestRateModelList = entry.getValue();

            // 计算平均慢请求率
            double sum = slowRequestRateModelList.stream().mapToDouble(SlowRequestRateModel::getSlowRequestRate).sum();
            double average = sum / slowRequestRateModelList.size();
            SlowRequestRateModel slowRequestRateModel = new SlowRequestRateModel();

            // 设置2025年第2周（ISO标准周计算）
            LocalDate date = LocalDate.of(2025,  1, 1)
                    .with(WeekFields.ISO.weekOfYear(),  entry.getKey())
                    .with(WeekFields.ISO.dayOfWeek(),  3); // 周三

            slowRequestRateModel.setDate(date.format(DateTimeFormatter.ISO_DATE));
            slowRequestRateModel.setPeriod(entry.getKey());
            slowRequestRateModel.setSlowRequestRate(average);
            averageSlowRequestRateModels.add(slowRequestRateModel);
        }
        return averageSlowRequestRateModels;
    }

    private List<SlowRequestRateModel> getSlowRequestRateModels(ExcelModel data) {
        List<List<String>> dataLists = data.getSheetModels().get(0).getData();
        List<SlowRequestRateModel> slowRequestRateModels = new ArrayList<>();
        // 使用ISO周规则计算周数
        WeekFields weekFields = WeekFields.ISO;
        // 写入数据
        for (int i = 0; i < dataLists.size() - 1; i++) {
            List<String> list = dataLists.get(i + 1);
            String dateString = list.get(0);
            double dateDouble = Double.parseDouble(dateString);
            LocalDate localDate = getLocalDate(dateDouble);
            int weekNumber = localDate.get(weekFields.weekOfWeekBasedYear());
            SlowRequestRateModel slowRequestRateModel = new SlowRequestRateModel();
            slowRequestRateModel.setDate(getDateString(dateDouble));
            slowRequestRateModel.setPeriod(weekNumber);
            slowRequestRateModel.setSlowRequestRate(Float.parseFloat(list.get(1)));
            slowRequestRateModels.add(slowRequestRateModel);
        }
        return slowRequestRateModels;
    }

    private static List<P99Model> getAverageP99Models(List<P99Model> p99Models) {
        // 将p99Models按照周数分组，组内的顺序按照日期排序，并计算平均值
        Map<Integer, List<P99Model>> groupedP99Models = p99Models.stream().collect(Collectors.groupingBy(P99Model::getPeriod));
        List<P99Model> averageP99Models = new ArrayList<>();
        for (Map.Entry<Integer, List<P99Model>> entry : groupedP99Models.entrySet()) {
            List<P99Model> p99ModelList = entry.getValue();
            int sum = p99ModelList.stream().mapToInt(P99Model::getP99).sum();
            int average = sum / p99ModelList.size();
            P99Model averageP99Model = new P99Model();

            // 设置2025年第2周（ISO标准周计算）
            LocalDate date = LocalDate.of(2025,  1, 1)
                    .with(WeekFields.ISO.weekOfYear(),  entry.getKey())
                    .with(WeekFields.ISO.dayOfWeek(),  3); // 周三

            averageP99Model.setDate(date.format(DateTimeFormatter.ISO_DATE));
            averageP99Model.setPeriod(entry.getKey());
            averageP99Model.setP99(average);
            averageP99Models.add(averageP99Model);
        }
        return averageP99Models;
    }

    private static List<P99Model> getP99Models(ExcelModel data) {
        List<List<String>> dataLists = data.getSheetModels().get(0).getData();
        List<P99Model> p99Models = new ArrayList<>();
        // 使用ISO周规则计算周数
        WeekFields weekFields = WeekFields.ISO;
        // 写入数据
        for (int i = 0; i < dataLists.size() - 1; i++) {
            List<String> list = dataLists.get(i + 1);
            String dateString = list.get(0);
            double dateDouble = Double.parseDouble(dateString);
            LocalDate localDate = getLocalDate(dateDouble);
            int weekNumber = localDate.get(weekFields.weekOfWeekBasedYear());
            P99Model p99Model = new P99Model();
            p99Model.setDate(getDateString(dateDouble));
            p99Model.setPeriod(weekNumber);
            p99Model.setP99(convertStringToInteger(list.get(3)));
            p99Models.add(p99Model);
        }
        return p99Models;
    }

    private static String getDateString(double excelSerialNumber) {
        LocalDate targetDate = getLocalDate(excelSerialNumber);
        return targetDate.format(DateTimeFormatter.ISO_DATE);
    }

    private static LocalDate getLocalDate(double excelSerialNumber) {
        int days = (int) excelSerialNumber;

        // Excel日期起点：1900-01-01（序列号1对应1900-01-01）
        LocalDate baseDate = LocalDate.of(1900, 1, 1);

        // 调整Excel的闰年误差（1900年非闰年，但Excel视为闰年）
        if (days > 60) {
            days -= 1;
        }

        // 计算实际日期（注意：Excel序列号从1开始，需减1天）
        LocalDate targetDate = baseDate.plusDays(days - 1);
        return targetDate;
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
