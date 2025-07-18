package com.jungo.diy.controller;

import com.jungo.diy.constants.FileConstants;
import com.jungo.diy.model.BusinessStatusErrorModel;
import com.jungo.diy.model.UrlStatusErrorModel;
import com.jungo.diy.service.FileService;
import com.jungo.diy.test.ElasticsearchQuery;
import com.jungo.diy.util.CsvUtils;
import com.jungo.diy.util.FileUtils;
import com.jungo.diy.util.TableUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jungo.diy.constants.FileConstants.BUSINESS_COLUMN_TITLES;
import static com.jungo.diy.constants.FileConstants.STATUS_COLUMN_TITLES;
import static com.jungo.diy.constants.FileConstants.SUCCESS_COLUMN_TITLES;

/**
 * @author lichuang3
 * @date 2025-04-25 15:31
 */
@Api(tags = "接口错误率统计")
@RestController
@Slf4j
@RequestMapping("/urlErrorRate")
public class UrlErrorRateController {

    @Autowired
    private FileService fileService;

    private void generateExcelFile(List<UrlStatusErrorModel> statusModels, List<BusinessStatusErrorModel> businessModels) throws IOException {

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            createStatusSheet(workbook, statusModels);
            createBusinessSheet(workbook, businessModels);
            createSuccessSheet(workbook, businessModels);

            String fileName = URLEncoder.encode("httpError.xlsx", StandardCharsets.UTF_8.toString());
            saveWorkbookToFile(workbook, FileConstants.OUTPUT_DIRECTORY, fileName);
        }
    }

    /**
     * 从完整URL中匹配指定路径
     * @param fullUrl 完整URL 如 "/cl-tire-site/tireModule/getTireDetailModuleData"
     * @param shortPath 要匹配的路径 如 "/tireModule/getTireDetailModuleData"
     * @return 是否匹配
     */
    public static boolean matchUrl(String fullUrl, String shortPath) {
        if (fullUrl == null || shortPath == null) {
            return false;
        }

        // 去除两边的斜杠统一格式
        String normalizedFull = fullUrl.replaceAll("^/+|/+$", "");
        String normalizedShort = shortPath.replaceAll("^/+|/+$", "");

        // 以短路径结尾进行匹配
        return normalizedFull.endsWith(normalizedShort);
    }

    private void createSuccessSheet(XSSFWorkbook workbook, List<BusinessStatusErrorModel> models) {

        XSSFSheet sheet = workbook.createSheet("成功率");
        TableUtils.createChartData(workbook, sheet, models, SUCCESS_COLUMN_TITLES,
                (model, col, cell) -> {
                    switch (col) {
                        case 0: cell.setCellValue(model.getAppId()); break;
                        case 1: cell.setCellValue(model.getUrl()); break;
                        case 2: cell.setCellValue(model.getTotalRequests()); break;
                        case 3: cell.setCellValue(model.getErrorRequests()); break;
                        case 4: cell.setCellValue(model.getErrorRate()); break;
                        case 5: cell.setCellValue(model.getNormalRequestRate()); break;
                        case 6: cell.setCellValue(model.getNot200errorRate()); break;
                        case 7: cell.setCellValue(model.getSuccessRate()); break;
                    }
                });
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
                        case 5: cell.setCellValue(model.getNormalRequestRate()); break;
                    }
                });
    }

    /**
     * 获取接口错误率数据并生成Excel报表
     * @param accesslogFile 访问日志文件
     * @param codeFile 业务错误码文件
     * @param date 统计日期(yyyy-MM-dd)
     * @return 处理结果
     * @throws IOException 文件处理异常
     */
    @ApiOperation(value = "获取接口错误率数据", notes = "处理访问日志和业务错误码文件，生成包含状态码错误率和业务异常错误率的Excel报表")
    @PostMapping("/obtainErrorRateData")
    public ResponseEntity<String> obtainErrorRateData(@ApiParam(value = "accesslog", required = true)
                                                      @RequestParam("accesslogFile") MultipartFile accesslogFile,

                                                      @ApiParam(value = "code", required = true)
                                                      @RequestParam("codeFile") MultipartFile codeFile,

                                                      @ApiParam(value = "以yyyy-MM-dd格式表示的日期", required = true)
                                                      @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) throws IOException {
        try {
            FileUtils.ensureDirectoryExists(FileConstants.OUTPUT_DIRECTORY);

            List<UrlStatusErrorModel> statusModels = fileService.processAccessLogFile(accesslogFile);
            List<BusinessStatusErrorModel> businessModels = fileService.processCodeFile(codeFile, date);

            businessModels.forEach(model -> {
                String url = model.getUrl();
                UrlStatusErrorModel urlStatusErrorModel = statusModels.stream().filter(x -> matchUrl(x.getUrl(), url)).findFirst().orElse(null);
                if (urlStatusErrorModel != null) {
                    model.setNot200errorRate(urlStatusErrorModel.getNot200errorRate());
                }
            });

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
