package com.jungo.diy.service;

import com.jungo.diy.response.UrlPerformanceResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;

/**
 * @author lichuang3
 */
@Service
public class ExportService {

    public void exportToExcel(List<UrlPerformanceResponse> criticalLinkUrlPerformanceResponses,
                              List<UrlPerformanceResponse> fiveGangJingUrlPerformanceResponses,
                              List<UrlPerformanceResponse> firstScreenTabUrlPerformanceResponses,
                              List<UrlPerformanceResponse> qilinComponentInterfaceUrlPerformanceResponses,
                              List<UrlPerformanceResponse> otherCoreBusinessInterfaceUrlPerformanceResponses,
                              List<UrlPerformanceResponse> accessVolumeTop30Interface,
                              HttpServletResponse response) throws IOException {
        // 创建工作簿
        Workbook workbook = new XSSFWorkbook();

        // 创建第一个 Sheet 并写入数据
        createSheet(workbook, "关键链路", criticalLinkUrlPerformanceResponses);
        // 创建第二个 Sheet 并写入数据
        createSheet(workbook, "五大金刚", fiveGangJingUrlPerformanceResponses);
        // 创建第三个 Sheet 并写入数据
        createSheet(workbook, "首屏Tab", firstScreenTabUrlPerformanceResponses);
        // 创建第四个 Sheet 并写入数据
        createSheet(workbook, "麒麟组件接口", qilinComponentInterfaceUrlPerformanceResponses);
        // 创建第五个 Sheet 并写入数据
        createSheet(workbook, "其他核心业务接口", otherCoreBusinessInterfaceUrlPerformanceResponses);
        // 创建第六个 Sheet 并写入数据
        createSheet(workbook, "访问量top30接口", accessVolumeTop30Interface);

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("export.xlsx", "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName);

        // 输出文件
        OutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        workbook.close();
        outputStream.flush();
        outputStream.close();
    }

    private void createSheet(Workbook workbook, String sheetName, List<UrlPerformanceResponse> data) {
        // 创建 Sheet
        Sheet sheet = workbook.createSheet(sheetName);

        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"host", "url", "上周99线", "本周99线", "上周请求总数", "本周请求总数", "上周慢请求率", "本周慢请求率", "99线变化", "99线变化率"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

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
            row.createCell(6).setCellValue(urlPerformanceResponse.getLastWeekSlowRequestRate());
            row.createCell(7).setCellValue(urlPerformanceResponse.getThisWeekSlowRequestRate());
            row.createCell(8).setCellValue(urlPerformanceResponse.getP99Change());
            row.createCell(9).setCellValue(urlPerformanceResponse.getP99ChangeRate());
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}