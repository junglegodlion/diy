package com.jungo.diy.controller;

import com.jungo.diy.model.BusinessStatusErrorModel;
import com.jungo.diy.model.UrlStatusErrorModel;
import com.jungo.diy.util.CsvUtils;
import com.jungo.diy.util.TableUtils;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lichuang3
 * @date 2025-04-25 15:31
 */
@RestController
@Slf4j
@RequestMapping("/urlErrorRate")
public class UrlErrorRateController {

    @PostMapping("/upload/businessError")
    public String readCodeFile(@RequestParam("code") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }

        List<BusinessStatusErrorModel> businessStatusErrorModels = getBusinessStatusErrorModels(file);


        return "ok";
    }

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

    @PostMapping("/upload/statusError")
    public String readFile(@ApiParam(value = "accesslog", required = true)
                           @RequestParam("file1") MultipartFile file1,

                           @ApiParam(value = "code", required = true)
                           @RequestParam("file2") MultipartFile file2) throws IOException {
        String directoryPath = System.getProperty("user.home") + "/Desktop/备份/c端网关接口性能统计/数据统计/输出/";
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            // 创建所有必要的目录
            directory.mkdirs();
        }

        List<List<String>> listList = CsvUtils.getDataFromInputStream(file1.getInputStream());
        List<UrlStatusErrorModel> urlStatusErrorModels = new ArrayList<>();
        for (int i = 1; i < listList.size(); i++) {
            List<String> list = listList.get(i);
            UrlStatusErrorModel urlStatusErrorModel = new UrlStatusErrorModel();
            urlStatusErrorModel.setHost(list.get(0));
            urlStatusErrorModel.setUrl(list.get(1));
            urlStatusErrorModel.setStatus(Integer.parseInt(list.get(2)));
            urlStatusErrorModel.setCount(Integer.parseInt(list.get(3)));
            urlStatusErrorModels.add(urlStatusErrorModel);
        }

        // urlStatusErrorModels按照url进行分组
        Map<String, List<UrlStatusErrorModel>> urlStatusErrorModelMap = urlStatusErrorModels.stream()
                .collect(Collectors.groupingBy(UrlStatusErrorModel::getUrl));

        List<UrlStatusErrorModel> newUrlStatusErrorModels = new ArrayList<>();
        for (Map.Entry<String, List<UrlStatusErrorModel>> entry : urlStatusErrorModelMap.entrySet()) {
            List<UrlStatusErrorModel> value = entry.getValue();
            // 统计每个url的总请求数
            int totalCount = 0;
            int not200Count = 0;
            for (UrlStatusErrorModel urlStatusErrorModel : value) {
                Integer count = urlStatusErrorModel.getCount();
                totalCount = totalCount + count;
                if (urlStatusErrorModel.getStatus() != 200) {
                    not200Count = not200Count + count;
                }
            }
            int finalTotalCount = totalCount;
            int finalNot200Count = not200Count;
            value.forEach(urlStatusErrorModel -> {
                urlStatusErrorModel.setTotalCount(finalTotalCount);
                urlStatusErrorModel.setNot200Count(finalNot200Count);
                newUrlStatusErrorModels.add(urlStatusErrorModel);
            });

        }
        List<String> urlList = new ArrayList<>();
        urlList.add("/cl-tire-site/tireListModule/getTireList");
        urlList.add("/cl-maint-api/maintMainline/getBasicMaintainData");
        urlList.add("/cl-maint-mainline/mainline/getDynamicData");
        urlList.add("/cl-oto-front-api/batteryList/getBatteryList");
        urlList.add("/mlp-product-search-api/module/search/pageList");
        urlList.add("/mlp-product-search-api/main/search/api/mainProduct");
        urlList.add("/ext-website-cl-beauty-api/channelPage/v4/getBeautyHomeShopListAndRecommendLabel");
        urlList.add("/cl-product-components/GoodsDetail/detailModuleInfo");
        urlList.add("/cl-tire-site/tireModule/getTireDetailModuleData");
        urlList.add("/cl-maint-mainline/productMainline/getMaintProductDetailInfo");
        urlList.add("/cl-product-components/GoodsDetail/productDetailModularInfoForBff");
        urlList.add("/cl-ordering-aggregator/ordering/getOrderConfirmFloatLayerData");
        urlList.add("/cl-maint-order-create/order/getConfirmOrderData");

        // 将newUrlStatusErrorModels按照url排序，url按照urlList的顺序排序,如果url相同，再按照status排序
        newUrlStatusErrorModels.sort((model1, model2) -> {
            int index1 = urlList.indexOf(model1.getUrl());
            int index2 = urlList.indexOf(model2.getUrl());
            if (index1 != index2) {
                return Integer.compare(index1, index2);
            }
            return Integer.compare(model1.getStatus(), model2.getStatus());
        });
        List<BusinessStatusErrorModel> businessStatusErrorModels = getBusinessStatusErrorModels(file2);
        List<String> urlList2 = new ArrayList<>();
        urlList2.add("/tireListModule/getTireList");
        urlList2.add("/maintMainline/getBasicMaintainData");
        urlList2.add("/mainline/getDynamicData");
        urlList2.add("/batteryList/getBatteryList");
        urlList2.add("/module/search/pageList");
        urlList2.add("/main/search/api/mainProduct");
        urlList2.add("/channelPage/v4/getBeautyHomeShopListAndRecommendLabel");
        urlList2.add("/GoodsDetail/detailModuleInfo");
        urlList2.add("/getTireDetailModuleData");
        urlList2.add("/productMainline/getMaintProductDetailInfo");
        urlList2.add("/GoodsDetail/productDetailModularInfoForBff");
        urlList2.add("/ordering/getOrderConfirmFloatLayerData");
        urlList2.add("/order/getConfirmOrderData");
        businessStatusErrorModels.sort((model1, model2) -> {
            int index1 = urlList2.indexOf(model1.getUrl());
            int index2 = urlList2.indexOf(model2.getUrl());
            if (index1 != index2) {
                return Integer.compare(index1, index2);
            }
            return Float.compare(model1.getErrorRate(), model2.getErrorRate());
        });
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            // 定义 Sheet 名称和数据列表
            XSSFSheet sheet1 = workbook.createSheet("状态码错误率");
            XSSFSheet sheet2 = workbook.createSheet("业务异常错误率");
            String[] columnTitles = {"host", "url", "status", "请求次数", "请求总数", "占比", "非200占比"};
            String[] columnTitles2 = {"服务名称", "接口路径", "总请求量", "非10000请求量", "占比"};

            TableUtils.createChartData(workbook, sheet2, businessStatusErrorModels, columnTitles2, (model, columnIndex, cell) -> {
                switch (columnIndex) {
                    case 0:
                        cell.setCellValue(model.getAppId());
                        break;
                    case 1:
                        cell.setCellValue(model.getUrl());
                        break;
                    case 2:
                        cell.setCellValue(model.getTotalRequests());
                        break;
                    case 3:
                        cell.setCellValue(model.getErrorRequests());
                        break;
                    case 4:
                        cell.setCellValue(model.getErrorRate());
                        break;
                }
            });

            TableUtils.createChartData(workbook, sheet1, newUrlStatusErrorModels, columnTitles, (model, columnIndex, cell) -> {
                switch (columnIndex) {
                    case 0:
                        cell.setCellValue(model.getHost());
                        break;
                    case 1:
                        cell.setCellValue(model.getUrl());
                        break;
                    case 2:
                        cell.setCellValue(model.getStatus());
                        break;
                    case 3:
                        cell.setCellValue(model.getCount());
                        break;
                    case 4:
                        cell.setCellValue(model.getTotalCount());
                        break;
                    case 5:
                        cell.setCellValue(model.getPercentRate());
                        break;
                    case 6:
                        cell.setCellValue(model.getNot200errorRate());
                        break;
                }
            });


            String fileName = URLEncoder.encode("httpError.xlsx", StandardCharsets.UTF_8.toString());
            saveWorkbookToFile(workbook, directoryPath, fileName);
        }
        return "ok";
    }


    private void saveWorkbookToFile(XSSFWorkbook workbook, String directoryPath, String fileName) {
        String filePath = directoryPath + "/" + fileName;
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        } catch (IOException e) {
            log.error("文件保存失败 | 路径: {} | 错误: {}", filePath, e.getMessage(), e);
        }
    }
}
