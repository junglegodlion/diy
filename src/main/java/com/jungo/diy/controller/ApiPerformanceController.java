package com.jungo.diy.controller;

import com.jungo.diy.constants.FileConstants;
import com.jungo.diy.entity.ApiDailyPerformanceEntity;
import com.jungo.diy.mapper.ApiDailyPerformanceMapper;
import com.jungo.diy.model.ExcelModel;
import com.jungo.diy.model.SheetModel;
import com.jungo.diy.model.UrlStatusErrorModel;
import com.jungo.diy.service.FileService;
import com.jungo.diy.util.DateUtils;
import com.jungo.diy.util.TableUtils;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static com.jungo.diy.constants.FileConstants.STATUS_COLUMN_TITLES;
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

    @PostMapping("/look")
    public void readFile() {
        List<List<ApiDailyPerformanceEntity>> lists = new ArrayList<>();
        for (String apiUrl : apiUrls) {
            List<ApiDailyPerformanceEntity> slowRequestRate = apiDailyPerformanceMapper.getSlowRequestRate(apiUrl, LocalDate.parse("2025-08-18"), LocalDate.parse("2025-08-24"));
            // 内部列表按时间排序
            slowRequestRate.sort(Comparator.comparing(ApiDailyPerformanceEntity::getDate));

            lists.add(slowRequestRate);
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            createNewSheet(workbook, lists, "平均性能");
            for (List<ApiDailyPerformanceEntity> list : lists) {
                String string = list.stream().findFirst().map(ApiDailyPerformanceEntity::getUrl).orElse(null);
                createSheet(workbook, list, generateSheetName(string));
            }
            saveWorkbookToFile(workbook, buildOutputDirectory(), buildOutputFileName());
        } catch (IOException e) {
            log.error("生成Excel文件失败", e);
        }


    }

    private String generateSheetName(String token) {
        String[] parts = token.split("/");
        String name = "";
        if (parts.length >= 2) {
            name = parts[parts.length - 2] + "_" + parts[parts.length - 1];
        }
        return name.length() > 31 ? name.substring(0, 28) + "..." : name;
    }

    private String buildOutputFileName() throws UnsupportedEncodingException {
        return URLEncoder.encode("20250825.xlsx", StandardCharsets.UTF_8.toString());
    }
    private String buildOutputDirectory() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        return FileConstants.OUTPUT_DIRECTORY + "/" + dateStr;
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

    private void saveWorkbookToFile(XSSFWorkbook workbook, String directoryPath, String fileName) throws IOException {
// 确保目录存在，如果不存在则创建
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                log.warn("无法创建目录: {}", directoryPath);
            }
        }

        File file = new File(directory, fileName);
        try (FileOutputStream out = new FileOutputStream(file)) {
            workbook.write(out);
        }
    }
}
