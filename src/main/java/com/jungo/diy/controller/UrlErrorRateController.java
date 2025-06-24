package com.jungo.diy.controller;

import com.jungo.diy.model.BusinessStatusErrorModel;
import com.jungo.diy.model.UrlStatusErrorModel;
import com.jungo.diy.test.ElasticsearchQuery;
import com.jungo.diy.util.CsvUtils;
import com.jungo.diy.util.TableUtils;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author lichuang3
 * @date 2025-04-25 15:31
 */
@RestController
@Slf4j
@RequestMapping("/urlErrorRate")
public class UrlErrorRateController {

    private static final String[] STATUS_CODE_URLS = {
            "/cl-tire-site/tireListModule/getTireList",
            "/cl-maint-api/maintMainline/getBasicMaintainData",
            "/cl-maint-mainline/mainline/getDynamicData",
            "/cl-oto-front-api/batteryList/getBatteryList",
            "/mlp-product-search-api/module/search/pageList",
            "/mlp-product-search-api/main/search/api/mainProduct",
            "/ext-website-cl-beauty-api/channelPage/v4/getBeautyHomeShopListAndRecommendLabel",
            "/cl-product-components/GoodsDetail/detailModuleInfo",
            "/cl-tire-site/tireModule/getTireDetailModuleData",
            "/cl-maint-mainline/productMainline/getMaintProductDetailInfo",
            "/cl-product-components/GoodsDetail/productDetailModularInfoForBff",
            "/cl-ordering-aggregator/ordering/getOrderConfirmFloatLayerData",
            "/cl-maint-order-create/order/getConfirmOrderData"
    };

    private static final String[] BUSINESS_ERROR_URLS = {
            "/tireListModule/getTireList",
            "/maintMainline/getBasicMaintainData",
            "/mainline/getDynamicData",
            "/batteryList/getBatteryList",
            "/module/search/pageList",
            "/main/search/api/mainProduct",
            "/channelPage/v4/getBeautyHomeShopListAndRecommendLabel",
            "/GoodsDetail/detailModuleInfo",
            "/getTireDetailModuleData",
            "/productMainline/getMaintProductDetailInfo",
            "/GoodsDetail/productDetailModularInfoForBff",
            "/ordering/getOrderConfirmFloatLayerData",
            "/order/getConfirmOrderData"
    };

    private static final String OUTPUT_DIRECTORY = System.getProperty("user.home") +
            "/Desktop/备份/c端网关接口性能统计/数据统计/输出/";
    private static final String[] STATUS_COLUMN_TITLES = {
            "host", "url", "status", "请求次数", "请求总数", "占比", "非200占比"
    };
    private static final String[] BUSINESS_COLUMN_TITLES = {
            "服务名称", "接口路径", "总请求量", "非10000请求量", "占比"
    };


    @Autowired
    private ElasticsearchQuery elasticsearchQuery;

    private static List<BusinessStatusErrorModel> getBusinessStatusErrorModels(MultipartFile file) throws IOException {
        List<BusinessStatusErrorModel> businessStatusErrorModels = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // line按照间隙进行分割
                String[] parts = line.split(" ");
                if (parts.length < 6) {
                    continue;
                }
                BusinessStatusErrorModel businessStatusError = new BusinessStatusErrorModel();
                businessStatusError.setAppId(parts[0]);
                businessStatusError.setUrl(parts[1]);
                businessStatusError.setTotalRequests(Integer.parseInt(parts[3]));
                businessStatusError.setErrorRequests(Integer.parseInt(parts[5]));
                businessStatusErrorModels.add(businessStatusError);
            }
        }
        return businessStatusErrorModels;
    }

    private void ensureDirectoryExists(String path) throws IOException {
        File directory = new File(path);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("无法创建目录: " + path);
        }
    }

    private List<UrlStatusErrorModel> processAccesslogFile(MultipartFile file) throws IOException {
        List<List<String>> csvData = CsvUtils.getDataFromInputStream(file.getInputStream());

        return csvData.stream()
                .skip(1)
                .map(this::mapToUrlStatusErrorModel)
                .collect(Collectors.groupingBy(UrlStatusErrorModel::getUrl))
                .entrySet().stream()
                .flatMap(this::processUrlStatusGroup)
                .sorted(this::compareUrlStatusModels)
                .collect(Collectors.toList());
    }

    private int compareUrlStatusModels(UrlStatusErrorModel m1, UrlStatusErrorModel m2) {
        int index1 = Arrays.asList(STATUS_CODE_URLS).indexOf(m1.getUrl());
        int index2 = Arrays.asList(STATUS_CODE_URLS).indexOf(m2.getUrl());
        return index1 != index2 ? Integer.compare(index1, index2) :
                Integer.compare(m1.getStatus(), m2.getStatus());
    }

    private Stream<UrlStatusErrorModel> processUrlStatusGroup(
            Map.Entry<String, List<UrlStatusErrorModel>> entry) {

        List<UrlStatusErrorModel> models = entry.getValue();
        int totalCount = models.stream().mapToInt(UrlStatusErrorModel::getCount).sum();
        int not200Count = models.stream()
                .filter(m -> m.getStatus() != 200)
                .mapToInt(UrlStatusErrorModel::getCount)
                .sum();

        return models.stream().peek(m -> {
            m.setTotalCount(totalCount);
            m.setNot200Count(not200Count);
        });
    }

    private UrlStatusErrorModel mapToUrlStatusErrorModel(List<String> row) {
        UrlStatusErrorModel model = new UrlStatusErrorModel();
        model.setHost(row.get(0));
        model.setUrl(row.get(1));
        model.setStatus(Integer.parseInt(row.get(2)));
        model.setCount(Integer.parseInt(row.get(3)));
        return model;
    }

    private List<BusinessStatusErrorModel> processCodeFile(MultipartFile file, LocalDate date) throws IOException {

        List<BusinessStatusErrorModel> models = getBusinessStatusErrorModels(file);

        models.stream()
                .filter(x -> "/maintMainline/getBasicMaintainData".equals(x.getUrl()))
                .findFirst()
                .ifPresent(x -> x.setErrorRequests(
                        elasticsearchQuery.getTotal(
                                "ext-website-cl-maint-api",
                                "/maintMainline/getBasicMaintainData",
                                date
                        )
                ));

        models.sort(this::compareBusinessModels);
        return models;
    }

    private int compareBusinessModels(BusinessStatusErrorModel m1, BusinessStatusErrorModel m2) {
        int index1 = Arrays.asList(BUSINESS_ERROR_URLS).indexOf(m1.getUrl());
        int index2 = Arrays.asList(BUSINESS_ERROR_URLS).indexOf(m2.getUrl());
        return index1 != index2 ? Integer.compare(index1, index2) :
                Float.compare(m1.getErrorRate(), m2.getErrorRate());
    }

    private void generateExcelFile(List<UrlStatusErrorModel> statusModels, List<BusinessStatusErrorModel> businessModels) throws IOException {

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            createStatusSheet(workbook, statusModels);
            createBusinessSheet(workbook, businessModels);

            String fileName = URLEncoder.encode("httpError.xlsx", StandardCharsets.UTF_8.toString());
            saveWorkbookToFile(workbook, OUTPUT_DIRECTORY, fileName);
        }
    }

    private void createStatusSheet(XSSFWorkbook workbook, List<UrlStatusErrorModel> models) {

        XSSFSheet sheet = workbook.createSheet("状态码错误率");
        TableUtils.createChartData(workbook, sheet, models, STATUS_COLUMN_TITLES,
                (model, col, cell) -> {
                    switch (col) {
                        case 0: cell.setCellValue(model.getHost()); break;
                        case 1: cell.setCellValue(model.getUrl()); break;
                        case 2: cell.setCellValue(model.getStatus()); break;
                        case 3: cell.setCellValue(model.getCount()); break;
                        case 4: cell.setCellValue(model.getTotalCount()); break;
                        case 5: cell.setCellValue(model.getPercentRate()); break;
                        case 6: cell.setCellValue(model.getNot200errorRate()); break;
                    }
                });
    }


    private void createBusinessSheet(XSSFWorkbook workbook, List<BusinessStatusErrorModel> models) {

        XSSFSheet sheet = workbook.createSheet("业务异常错误率");
        TableUtils.createChartData(workbook, sheet, models, BUSINESS_COLUMN_TITLES,
                (model, col, cell) -> {
                    switch (col) {
                        case 0: cell.setCellValue(model.getAppId()); break;
                        case 1: cell.setCellValue(model.getUrl()); break;
                        case 2: cell.setCellValue(model.getTotalRequests()); break;
                        case 3: cell.setCellValue(model.getErrorRequests()); break;
                        case 4: cell.setCellValue(model.getErrorRate()); break;
                    }
                });
    }

    @PostMapping("/upload/statusError")
    public ResponseEntity<String> readFile(@ApiParam(value = "accesslog", required = true)
                           @RequestParam("accesslogFile") MultipartFile accesslogFile,

                           @ApiParam(value = "code", required = true)
                           @RequestParam("codeFile") MultipartFile codeFile,

                           @ApiParam(value = "以yyyy-MM-dd格式表示的日期", required = true)
                           @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) throws IOException {
        try {
            ensureDirectoryExists(OUTPUT_DIRECTORY);

            List<UrlStatusErrorModel> statusModels = processAccesslogFile(accesslogFile);
            List<BusinessStatusErrorModel> businessModels = processCodeFile(codeFile, date);
            generateExcelFile(statusModels, businessModels);

            return ResponseEntity.ok("文件处理成功");
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("文件处理失败: " + e.getMessage());
        }
    }


    private void saveWorkbookToFile(XSSFWorkbook workbook, String directoryPath, String fileName) throws IOException {

        File file = new File(directoryPath, fileName);
        try (FileOutputStream out = new FileOutputStream(file)) {
            workbook.write(out);
        }
    }
}
