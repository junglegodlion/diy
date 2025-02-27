package com.jungo.diy.service;

import com.jungo.diy.response.UrlPerformanceResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author lichuang3
 */
@Service
@Slf4j
public class ExportService {

    public void exportToExcel(List<UrlPerformanceResponse> criticalLinkUrlPerformanceResponses,
                              List<UrlPerformanceResponse> fiveGangJingUrlPerformanceResponses,
                              List<UrlPerformanceResponse> firstScreenTabUrlPerformanceResponses,
                              List<UrlPerformanceResponse> qilinComponentInterfaceUrlPerformanceResponses,
                              List<UrlPerformanceResponse> otherCoreBusinessInterfaceUrlPerformanceResponses,
                              List<UrlPerformanceResponse> accessVolumeTop30Interface,
                              HttpServletResponse response) throws IOException {
        // 创建工作簿
        try (Workbook workbook = new XSSFWorkbook()) {
            // 定义 Sheet 名称和数据列表
            String[] sheetNames = {"关键链路", "五大金刚", "首屏Tab", "麒麟组件接口", "其他核心业务接口", "访问量top30接口"};
            List<UrlPerformanceResponse>[] dataLists = new List[]{
                    criticalLinkUrlPerformanceResponses,
                    fiveGangJingUrlPerformanceResponses,
                    firstScreenTabUrlPerformanceResponses,
                    qilinComponentInterfaceUrlPerformanceResponses,
                    otherCoreBusinessInterfaceUrlPerformanceResponses,
                    accessVolumeTop30Interface
            };

            // 创建多个 Sheet 并写入数据
            for (int i = 0; i < sheetNames.length; i++) {
                createSheet(workbook, sheetNames[i], dataLists[i]);
            }

            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");

            // 获取当天日期并格式化为 yyyy-MM-dd 格式
            LocalDate currentDate = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String formattedDate = currentDate.format(formatter);
            String fileName = URLEncoder.encode(formattedDate + "_performance.xlsx", StandardCharsets.UTF_8.toString());
            response.setHeader("Content-disposition", "attachment;filename=" + fileName);

            // 拼接完整的文件路径
            String directoryPath = System.getProperty("user.home") + "/Desktop/备份/c端网关接口性能统计/数据统计/输出";
            String filePath = directoryPath + "/" + fileName;

            // 写入文件
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            } catch (IOException e) {
                log.error("ExportService#exportToExcel,出现异常！", e);
            }

            // 输出文件
            try (OutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
            }
        } catch (IOException e) {
            log.error("ExportService#exportToExcel,出现异常！", e);
        }
    }

    private void createSheet(Workbook workbook, String sheetName, List<UrlPerformanceResponse> data) {
        // 创建 Sheet
        Sheet sheet = workbook.createSheet(sheetName);

        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"host", "url", "上周99线", "本周99线", "上周请求总数", "本周请求总数", "上周慢请求率", "本周慢请求率", "99线变化", "99线变化率", "是否达标"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        // 创建百分比格式
        DataFormat dataFormat = workbook.createDataFormat();
        short percentageFormat = dataFormat.getFormat("0.00%");
        CellStyle percentageCellStyle = workbook.createCellStyle();
        percentageCellStyle.setDataFormat(percentageFormat);

        // 写入数据
        for (int i = 0; i < data.size(); i++) {
            Row row = sheet.createRow(i + 1);
            UrlPerformanceResponse urlPerformanceResponse = data.get(i);
            row.createCell(0).setCellValue(urlPerformanceResponse.getHost());
            row.createCell(1).setCellValue(urlPerformanceResponse.getUrl());
            row.createCell(2).setCellValue(urlPerformanceResponse.getLastWeekP99());
            row.createCell(3).setCellValue(urlPerformanceResponse.getThisWeekP99());
            row.createCell(4).setCellValue(urlPerformanceResponse.getLastWeekTotalRequestCount());
            row.createCell(5).setCellValue(urlPerformanceResponse.getThisWeekTotalRequestCount());

            Cell lastWeekSlowRequestRatePercentageCell = row.createCell(6);
            lastWeekSlowRequestRatePercentageCell.setCellValue(urlPerformanceResponse.getLastWeekSlowRequestRate());
            lastWeekSlowRequestRatePercentageCell.setCellStyle(percentageCellStyle);

            Cell thisWeekSlowRequestRatePercentageCell = row.createCell(7);
            thisWeekSlowRequestRatePercentageCell.setCellValue(urlPerformanceResponse.getThisWeekSlowRequestRate());
            thisWeekSlowRequestRatePercentageCell.setCellStyle(percentageCellStyle);

            row.createCell(8).setCellValue(urlPerformanceResponse.getP99Change());

            Cell p99ChangeRateCell = row.createCell(9);
            p99ChangeRateCell.setCellValue(urlPerformanceResponse.getP99ChangeRate());
            p99ChangeRateCell.setCellStyle(percentageCellStyle);
            row.createCell(10).setCellValue(urlPerformanceResponse.getReachTarget());
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}