package com.jungo.diy.service;

import com.jungo.diy.model.BusinessStatusErrorModel;
import com.jungo.diy.model.ExcelModel;
import com.jungo.diy.model.SheetModel;
import com.jungo.diy.model.UrlStatusErrorModel;
import com.jungo.diy.test.ElasticsearchQuery;
import com.jungo.diy.util.CsvUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jungo.diy.constants.FileConstants.BUSINESS_ERROR_URLS;
import static com.jungo.diy.constants.FileConstants.STATUS_CODE_URLS;

/**
 * @author lichuang3
 * @date 2025-02-06 12:55
 */
@Service
@Slf4j
public class FileService {

    @Autowired
    private ElasticsearchQuery elasticsearchQuery;

    public ExcelModel readXlsxFile(MultipartFile file) {

        ExcelModel excelModel = new ExcelModel();
        // 通过文件流创建工作簿
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            List<SheetModel> sheetModels = new ArrayList<>();
            int numberOfSheets = workbook.getNumberOfSheets();
            for (int i = 0; i < numberOfSheets; i++) {
                SheetModel sheetModel = new SheetModel();
                Sheet sheet = workbook.getSheetAt(i);
                sheetModel.setSheetIndex(i);
                sheetModel.setSheetName(sheet.getSheetName());
                List<List<String>> data = new ArrayList<>();
                for (Row row : sheet) {
                    List<String> rowData = new ArrayList<>();
                    // 读取单元格内容
                    for (Cell cell : row) {
                        rowData.add(getCellValueAsString(cell));
                    }
                    data.add(rowData);
                }
                sheetModel.setData(data);
                sheetModels.add(sheetModel);
            }
            excelModel.setSheetModels(sheetModels);
        } catch (Exception e) {
            log.error("FileService#readXlsxFile,出现异常！", e);
        }

        return excelModel;
    }

    private String getCellValueAsString(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    public List<UrlStatusErrorModel> processAccessLogFile(MultipartFile accesslogFile) {
        List<List<String>> csvData = null;
        try {
            csvData = CsvUtils.getDataFromInputStream(accesslogFile.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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


    public List<BusinessStatusErrorModel> processCodeFile(MultipartFile file, LocalDate date) {

        List<com.jungo.diy.model.BusinessStatusErrorModel> models = getBusinessStatusErrorModels(file);

        models.stream()
                .filter(x -> "/maintMainline/getBasicMaintainData".equals(x.getUrl()))
                .findFirst()
                .ifPresent(x -> x.setErrorRequests(
                        elasticsearchQuery.getTotal(
                                "ext-website-cl-maint-api",
                                "/maintMainline/getBasicMaintainData",
                                date,
                                true,
                                "Code=1")
                ));

        models.add(getBusinessStatusErrorModel("ext-service-cl-list-aggregator", "/channel/getChannelModuleInfo", date));
        models.add(getBusinessStatusErrorModel("int-restful-mlp-product-search-api", "/module/search/pageListAndFilter", date));
        models.add(getBusinessStatusErrorModel("ext-website-cl-maint-api", "/mainline/maintenance/basic", date));

        models.sort(this::compareBusinessModels);
        return models;
    }

    private BusinessStatusErrorModel getBusinessStatusErrorModel(String appId, String url, LocalDate date) {

        BusinessStatusErrorModel businessStatusErrorModel = new BusinessStatusErrorModel();
        businessStatusErrorModel.setAppId(appId);
        businessStatusErrorModel.setUrl(url);
        businessStatusErrorModel.setTotalRequests(elasticsearchQuery.getTotal(appId, url, date, false, null));
        businessStatusErrorModel.setErrorRequests(elasticsearchQuery.getTotal(appId, url, date, true, "code=10000"));


        return businessStatusErrorModel;
    }

    private UrlStatusErrorModel mapToUrlStatusErrorModel(List<String> row) {
        UrlStatusErrorModel model = new UrlStatusErrorModel();
        model.setHost(row.get(0));
        model.setUrl(row.get(1));
        model.setStatus(Integer.parseInt(row.get(2)));
        model.setCount(Integer.parseInt(row.get(3)));
        return model;
    }


    private int compareBusinessModels(BusinessStatusErrorModel m1, BusinessStatusErrorModel m2) {
        int index1 = Arrays.asList(BUSINESS_ERROR_URLS).indexOf(m1.getUrl());
        int index2 = Arrays.asList(BUSINESS_ERROR_URLS).indexOf(m2.getUrl());
        return index1 != index2 ? Integer.compare(index1, index2) :
                Float.compare(m1.getErrorRate(), m2.getErrorRate());
    }

    private static List<BusinessStatusErrorModel> getBusinessStatusErrorModels(MultipartFile file) {
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return businessStatusErrorModels;
    }
}
