package com.jungo.diy.service;

import com.jungo.diy.entity.ApiDailyPerformanceEntity;
import com.jungo.diy.mapper.ApiDailyPerformanceMapper;
import com.jungo.diy.util.ApiUrlReader;
import com.jungo.diy.util.DateUtils;
import com.jungo.diy.util.FileUtils;
import com.jungo.diy.util.TableUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.jungo.diy.util.DateUtils.YYYY_MM_DD;

/**
 * @author lichuang3
 * @date 2025-08-29 14:00
 */
@Slf4j
@Service
public class ApiPerformanceService {

    @Autowired
    private ApiDailyPerformanceMapper apiDailyPerformanceMapper;

    public void generateWeeklyPerformanceReport(LocalDate date) {
        List<String> apiUrls = ApiUrlReader.readApiUrls("api-urls.txt");
        List<ApiDailyPerformanceEntity> apiPerformance = apiDailyPerformanceMapper.getApiListDailyPerformance(apiUrls, date);
        // 3. 构造 url -> 顺序索引的 Map
        Map<String, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < apiUrls.size(); i++) {
            orderMap.put(apiUrls.get(i), i);
        }

        // 4. 按照 apiUrls 中的顺序排序
        apiPerformance.sort(Comparator.comparingInt(
                e -> orderMap.getOrDefault(e.getUrl(), Integer.MAX_VALUE)
        ));
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet(DateUtils.getDateString(date, YYYY_MM_DD));
            List<String> titles = new ArrayList<>();
            titles.add("url");
            titles.add("p90");
            titles.add("p99");
            TableUtils.createChartData(workbook, sheet, apiPerformance, titles.toArray(new String[0]),
                    (model, col, cell) -> {
                        switch (col) {
                            case 0: cell.setCellValue(model.getUrl()); break;
                            case 1: cell.setCellValue(model.getP90()); break;
                            case 2: cell.setCellValue(model.getP99()); break;
                        }
                    });
            FileUtils.saveWorkbookToFile(workbook, FileUtils.buildOutputDirectory(LocalDate.now().format(DateTimeFormatter.ISO_DATE)), FileUtils.buildOutputFileName("RT-daily.xlsx"));
        } catch (IOException e) {
            log.error("生成Excel文件失败", e);
        }

    }

    public void generateQ3PerformanceReport() {
        List<ApiDailyPerformanceEntity> lists = new ArrayList<>();

        List<String> apiUrls = ApiUrlReader.readApiUrls("api-urls.txt");
        for (String apiUrl : apiUrls) {
            List<ApiDailyPerformanceEntity> slowRequestRate = apiDailyPerformanceMapper.getSlowRequestRate(apiUrl, LocalDate.parse("2025-07-01"), LocalDate.parse("2025-09-25"));
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

            lists.addAll(filteredAndSorted);
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            createNewSheet(workbook, lists, "Q3数据");
            FileUtils.saveWorkbookToFile(workbook, FileUtils.buildOutputDirectory(LocalDate.now().format(DateTimeFormatter.ISO_DATE)), FileUtils.buildOutputFileName("20250925.xlsx"));
        } catch (IOException e) {
            log.error("生成Excel文件失败", e);
        }
    }

    private void createNewSheet(XSSFWorkbook workbook, List<ApiDailyPerformanceEntity> lists, String sheetName) {

        XSSFSheet sheet = workbook.createSheet(sheetName);
        List<String> titlesWithAvg = new ArrayList<>();
        titlesWithAvg.add("url");
        titlesWithAvg.add("p90");
        titlesWithAvg.add("p99");
        titlesWithAvg.add("date");
        TableUtils.createChartData(workbook, sheet, lists, titlesWithAvg.toArray(new String[0]),
                (model, col, cell) -> {
                    switch (col) {
                        case 0:
                            cell.setCellValue(model.getUrl());
                            break;
                        case 1:
                            cell.setCellValue(model.getP90());
                            break;
                        case 2:
                            cell.setCellValue(model.getP99());
                            break;
                        case 3:
                            cell.setCellValue(DateUtils.getDateString(model.getDate(), YYYY_MM_DD));
                            break;
                    }
                });
    }
}
