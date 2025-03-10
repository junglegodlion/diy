package com.jungo.diy.controller;

import com.jungo.diy.model.*;
import com.jungo.diy.response.UrlPerformanceResponse;
import com.jungo.diy.service.ExportService;
import com.jungo.diy.service.FileService;
import com.jungo.diy.util.DateUtils;
import com.jungo.diy.util.PerformanceUtils;
import com.jungo.diy.util.TableUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.PresetColor;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDLbls;
import org.openxmlformats.schemas.drawingml.x2006.chart.STDLblPos;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

import static com.jungo.diy.util.DateUtils.YYYY_MM_DD;
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
        List<UrlPerformanceResponse>[] dataLists = new List[]{
                criticalLinkUrlPerformanceResponses,
                fiveGangJingUrlPerformanceResponses,
                firstScreenTabUrlPerformanceResponses,
                qilinComponentInterfaceUrlPerformanceResponses,
                otherCoreBusinessInterfaceUrlPerformanceResponses,
                accessVolumeTop30Interface
        };
        exportService.exportToExcel(dataLists, response);
    }


    /**
     * 处理文件上传并生成图表
     * @author lichuang3
     */
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

        // 99线-指定日期 如“2025-02-01”
        List<P99Model> p99Models = getAllP99Models(data, "2025-02-01");
        // 周维度99线
        List<P99Model> averageP99Models = PerformanceUtils.getAverageP99Models(getAllP99Models(data, null));
        // 慢请求率
        List<SlowRequestRateModel> slowRequestRateModels = getSlowRequestRateModels(data, "2025-02-01");
        // 周维度慢请求率
        List<SlowRequestRateModel> averageSlowRequestRateModels = PerformanceUtils.getAverageSlowRequestRateModels(getSlowRequestRateModels(data, null));
        // 画图
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // 定义 Sheet 名称和数据列表
            String[] sheetNames = {"99线", "周维度99线", "慢请求率", "周维度慢请求率"};

            createP99ModelSheet(workbook, sheetNames[0], p99Models, "gateway 99线", "日期", "99线", "99线");
            createP99ModelSheet(workbook, sheetNames[1], averageP99Models, "gateway 99线-周维度", "日期", "99线", "99线");
            createSlowRequestRateModelSheet(workbook, sheetNames[2], slowRequestRateModels, "gateway 慢请求率", "日期", "慢请求率", "慢请求率");
            createSlowRequestRateModelSheet(workbook, sheetNames[3], averageSlowRequestRateModels, "gateway 慢请求率-周维度", "日期", "慢请求率", "慢请求率");

            // 拼接完整的文件路径
            // 获取当天日期并格式化为 yyyy-MM-dd 格式
            LocalDate currentDate = LocalDate.now();
            String formattedDate = DateUtils.getDateString(currentDate, YYYY_MM_DD);
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

    public static void createSlowRequestRateModelSheet(XSSFWorkbook workbook,
                                                 String sheetName,
                                                 List<SlowRequestRateModel> slowRequestRateModels,
                                                 String titleText,
                                                 String xTitle,
                                                 String yTitle,
                                                 String seriesTitle) {
        String[] columnTitles = {"日期", "慢请求率"};
        TableUtils.createModelSheet(workbook, sheetName, slowRequestRateModels, columnTitles, titleText, xTitle, yTitle, seriesTitle, (model, columnIndex, cell) -> {
            switch (columnIndex) {
                case 0:
                    cell.setCellValue(model.getDate());
                    break;
                case 1:
                    cell.setCellValue(model.getSlowRequestRate());
                    cell.setCellStyle(TableUtils.getPercentageCellStyle(workbook));
                    break;
            }
        });

    }
    public static void createP99ModelSheet(XSSFWorkbook workbook,
                                     String sheetName,
                                     List<P99Model> p99Models,
                                     String titleText,
                                     String xTitle,
                                     String yTitle,
                                     String seriesTitle) {
        String[] columnTitles = {"日期", "99线"};
        TableUtils.createModelSheet(workbook, sheetName, p99Models, columnTitles, titleText, xTitle, yTitle, seriesTitle, (model, columnIndex, cell) -> {
            switch (columnIndex) {
                case 0:
                    cell.setCellValue(model.getDate());
                    break;
                case 1:
                    cell.setCellValue(model.getP99());
                    break;
            }
        });
    }

    public static void createP99ModelsData(XSSFSheet sheet, List<P99Model> p99Models) {
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

    public static void createSlowRequestRateModelsData(XSSFSheet sheet, List<SlowRequestRateModel> slowRequestRateModels) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("日期");
        headerRow.createCell(1).setCellValue("慢请求率");

        // 填充数据行
        List<String> dates = slowRequestRateModels.stream().map(SlowRequestRateModel::getDate).collect(Collectors.toList());
        List<Double> p99Values = slowRequestRateModels.stream().map(SlowRequestRateModel::getSlowRequestRate).collect(Collectors.toList());

        for (int i = 0; i < dates.size(); i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(dates.get(i));
            row.createCell(1).setCellValue(p99Values.get(i));
        }

    }



    private List<SlowRequestRateModel> getSlowRequestRateModels(ExcelModel data, String dateStr) {
        LocalDate specifyDate = null;
        if (StringUtils.isNotBlank(dateStr)) {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
            specifyDate = LocalDate.parse(dateStr, formatter);
        }
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
            if (specifyDate != null && localDate.isBefore(specifyDate)) {
                continue;
            }
            int weekNumber = localDate.get(weekFields.weekOfWeekBasedYear());
            SlowRequestRateModel slowRequestRateModel = new SlowRequestRateModel();
            slowRequestRateModel.setDate(getDateString(dateDouble));
            slowRequestRateModel.setPeriod(weekNumber);
            slowRequestRateModel.setSlowRequestRate(Float.parseFloat(list.get(1)));
            slowRequestRateModels.add(slowRequestRateModel);
        }
        return slowRequestRateModels;
    }



    private static List<P99Model> getAllP99Models(ExcelModel data, String dateStr) {
        LocalDate specifyDate = null;
        if (StringUtils.isNotBlank(dateStr)) {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
            specifyDate = LocalDate.parse(dateStr, formatter);
        }
        List<List<String>> dataLists = data.getSheetModels().get(0).getData();
        List<P99Model> p99Models = new ArrayList<>();

        // 写入数据
        for (int i = 0; i < dataLists.size() - 1; i++) {
            List<String> list = dataLists.get(i + 1);
            String dateString = list.get(0);
            double dateDouble = Double.parseDouble(dateString);
            LocalDate localDate = getLocalDate(dateDouble);
            if (specifyDate != null && localDate.isBefore(specifyDate)) {
                continue;
            }
            int weekNumber = DateUtils.getWeekNumber(localDate);
            P99Model p99Model = new P99Model();
            p99Model.setDate(getDateString(dateDouble));
            p99Model.setPeriod(weekNumber);
            p99Model.setP99(TableUtils.convertStringToInteger(list.get(3)));
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
        Map<String, Integer> slowRequestCountMap = slowRequestCountSheetModel.getData().stream().collect(Collectors.toMap(x -> x.get(0) + x.get(1), x -> TableUtils.convertStringToInteger(x.get(2)), (x, y) -> x));

        // 请求情况
        List<InterfacePerformanceModel> interfacePerformanceModels = new ArrayList<>();
        for (List<String> datum : requestsheetModel.getData()) {
            InterfacePerformanceModel interfacePerformanceModel = new InterfacePerformanceModel();
            String token = datum.get(0) + datum.get(1);
            interfacePerformanceModel.setToken(token);
            interfacePerformanceModel.setHost(datum.get(0));
            interfacePerformanceModel.setUrl(datum.get(1));
            interfacePerformanceModel.setTotalRequestCount(TableUtils.convertStringToInteger(datum.get(2)));
            interfacePerformanceModel.setP99(TableUtils.convertStringToInteger(datum.get(4)));
            Integer slowRequestCount = slowRequestCountMap.get(token);
            if (Objects.isNull(slowRequestCount)) {
                slowRequestCount = 0;
            }
            interfacePerformanceModel.setSlowRequestCount(slowRequestCount);
            interfacePerformanceModels.add(interfacePerformanceModel);
        }

        return interfacePerformanceModels;
    }


    private ExcelModel readXlsxFile(MultipartFile file) throws IOException {
        // XLSX 文件读取逻辑
        return fileService.readXlsxFile(file);
    }

    private List<List<String>> readCsvFile(MultipartFile file) throws IOException {
        // CSV 文件读取逻辑
        return fileService.readCsvFile(file);
    }
}
