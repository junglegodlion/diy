package com.jungo.diy.controller;

import com.jungo.diy.entity.ApiDailyPerformanceEntity;
import com.jungo.diy.mapper.ApiDailyPerformanceMapper;
import com.jungo.diy.service.ApiPerformanceService;
import com.jungo.diy.util.DateUtils;
import com.jungo.diy.util.FileUtils;
import com.jungo.diy.util.TableUtils;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.jungo.diy.constants.FileConstants.WEEKLY_PERFORMANCE_TITLES;
import static com.jungo.diy.util.DateUtils.YYYY_MM_DD;

/**
 * @author lichuang3
 * @date 2025-08-25 19:19
 */
@RestController
@Slf4j
@RequestMapping("/api")
public class ApiPerformanceController {

    @Autowired
    private ApiPerformanceService apiPerformanceService;

    List<String> apiUrls = Arrays.asList(
            "/cl-homepage-service/homePage/getHomePageInfo",
            "/cl-list-aggregator/channel/getChannelModuleInfo",
            "/mlp-product-search-api/module/search/pageListAndFilter",
            "/cl-tire-site/tireListModule/getTireList",
            "/cl-maint-api/maintMainline/getBasicMaintainData",
            "/cl-maint-mainline/mainline/getDynamicData",
            "/cl-maint-api/mainline/maintenance/basic",
            "/cl-oto-front-api/batteryList/getBatteryList",
            "/mlp-product-search-api/module/search/pageList",
            "/mlp-product-search-api/main/search/api/mainProduct",
            "/ext-website-cl-beauty-api/channelPage/v4/getBeautyHomeShopListAndRecommendLabel",
            "/cl-product-components/GoodsDetail/detailModuleInfo",
            "/cl-tire-site/tireModule/getTireDetailModuleData",
            "/cl-maint-mainline/productMainline/getMaintProductDetailInfo",
            "/cl-product-components/GoodsDetail/productDetailModularInfoForBff",
            "/cl-maint-order-create/order/getConfirmOrderData",
            "/mkt-platform-activity-page-service/activityPage/getActivityPageFirstScreen",
            "/cl-ordering-aggregator/ordering/getOrderConfirmFloatLayerData"
    );


    @Autowired
    private ApiDailyPerformanceMapper apiDailyPerformanceMapper;

    /**
     * 生成API性能报告Excel文件
     * 该方法会查询预定义API列表的性能数据，并生成包含汇总和详细信息的Excel报告
     */
    @PostMapping("/generate-weekly-performance-report")
    public void generateWeeklyPerformanceReport() {

        List<List<ApiDailyPerformanceEntity>> lists = new ArrayList<>();
        for (String apiUrl : apiUrls) {
            List<ApiDailyPerformanceEntity> slowRequestRate = apiDailyPerformanceMapper.getApiPerformance(apiUrl, LocalDate.parse("2025-09-22"), LocalDate.parse("2025-09-27"));
            // slowRequestRate存在日期相同的数据，保留totalRequestCount最大的那条数据
            // 按日期分组，保留每个日期中totalRequestCount最大的记录
            Map<Date, ApiDailyPerformanceEntity> maxByDate = slowRequestRate.stream()
                    .collect(Collectors.toMap(
                            ApiDailyPerformanceEntity::getDate,
                            Function.identity(),
                            (existing, replacement) ->
                                    existing.getTotalRequestCount() > replacement.getTotalRequestCount() ? existing : replacement
                    ));
            // 转换为列表并按日期排序
            List<ApiDailyPerformanceEntity> filteredAndSorted = new ArrayList<>(maxByDate.values());
            // 内部列表按时间排序
            filteredAndSorted.sort(Comparator.comparing(ApiDailyPerformanceEntity::getDate));

            lists.add(filteredAndSorted);
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            createNewSheet(workbook, lists, "平均性能");
            for (List<ApiDailyPerformanceEntity> list : lists) {
                String string = list.stream().findFirst().map(ApiDailyPerformanceEntity::getUrl).orElse(null);
                createSheet(workbook, list, generateSheetName(string));
            }
            FileUtils.saveWorkbookToFile(workbook, FileUtils.buildOutputDirectory(LocalDate.now().format(DateTimeFormatter.ISO_DATE)), FileUtils.buildOutputFileName("20250825.xlsx"));
        } catch (IOException e) {
            log.error("生成Excel文件失败", e);
        }
    }

    @PostMapping("/generate-Q3-performance-report")
    public void generateQ3PerformanceReport() {
        apiPerformanceService.generateQ3PerformanceReport();
    }

    /**
     * 生成API性能报告Excel文件
     * 该方法会查询预定义API列表的性能数据，并生成包含汇总和详细信息的Excel报告
     */
    @PostMapping("/generate-daily-performance-report")
    public void generateDailyPerformanceReport(@ApiParam(value = "具体日期", required = true) @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        apiPerformanceService.generateWeeklyPerformanceReport(date);
    }

    private String generateSheetName(String token) {
        String[] parts = token.split("/");
        String name = "";
        if (parts.length >= 2) {
            name = parts[parts.length - 2] + "_" + parts[parts.length - 1];
        }
        return name.length() > 31 ? name.substring(0, 28) + "..." : name;
    }




    private void createNewSheet(XSSFWorkbook workbook, List<List<ApiDailyPerformanceEntity>> lists,String sheetName) {

        XSSFSheet sheet = workbook.createSheet(sheetName);
        List<String> titlesWithAvg = new ArrayList<>();
        titlesWithAvg.add("url");
        titlesWithAvg.add("平均p90");
        titlesWithAvg.add("平均p99");
        TableUtils.createChartData(workbook, sheet, lists, titlesWithAvg.toArray(new String[0]),
                (model, col, cell) -> {
                    switch (col) {
                        case 0: cell.setCellValue(getUrl(model)); break;
                        case 1: cell.setCellValue(getP90Average(model)); break;
                        case 2: cell.setCellValue(getP99Average(model)); break;
                    }
                });
    }

    private int getP99Average(List<ApiDailyPerformanceEntity> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        return (int) Math.round(list.stream()
                .mapToDouble(ApiDailyPerformanceEntity::getP99)
                .filter(value -> value > 0)
                .average()
                .orElse(0.0));
    }

    private int getP90Average(List<ApiDailyPerformanceEntity> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        return (int) Math.round(list.stream()
                .mapToDouble(ApiDailyPerformanceEntity::getP90)
                .filter(value -> value > 0)
                .average()
                .orElse(0.0));
    }

    private String getUrl(List<ApiDailyPerformanceEntity> model) {

        return model.stream().findFirst().map(ApiDailyPerformanceEntity::getUrl).orElse(null);
    }

    private void createSheet(XSSFWorkbook workbook, List<ApiDailyPerformanceEntity> list,String sheetName) {

        XSSFSheet sheet = workbook.createSheet(sheetName);
        // 计算平均值
        double avgP90 = list.stream()
                .mapToDouble(ApiDailyPerformanceEntity::getP90)
                .filter(value -> value > 0) // 过滤无效值
                .average()
                .orElse(0.0);

        double avgP99 = list.stream()
                .mapToDouble(ApiDailyPerformanceEntity::getP99)
                .filter(value -> value > 0) // 过滤无效值
                .average()
                .orElse(0.0);
        // 创建包含平均值的标题行
        List<String> titlesWithAvg = new ArrayList<>(Arrays.asList(WEEKLY_PERFORMANCE_TITLES));
        titlesWithAvg.add("P90平均值: " + Math.round(avgP90));
        titlesWithAvg.add("P99平均值: " + Math.round(avgP99));

        TableUtils.createChartData(workbook, sheet, list, titlesWithAvg.toArray(new String[0]),
                (model, col, cell) -> {
                    switch (col) {
                        case 0: cell.setCellValue(model.getUrl()); break;
                        case 1: cell.setCellValue(model.getP90()); break;
                        case 2: cell.setCellValue(model.getP99()); break;
                        case 3:
                            if (model.getDate() != null) {
                                cell.setCellValue(DateUtils.getDateString(model.getDate(), YYYY_MM_DD));
                            }
                            break;
                    }
                });
    }


}
