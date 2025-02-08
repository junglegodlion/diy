package com.jungo.diy.controller;

import com.jungo.diy.service.ExcelChartService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
public class ExcelController {

    @GetMapping("/download/chart")
    public ResponseEntity<Resource> downloadExcelWithChart() throws IOException {
        // 1. 生成工作簿
        XSSFWorkbook workbook = ExcelChartService.generateExcelWithChart();
        
        // 2. 转换为字节流
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        
        // 3. 封装为Resource
        byte[] bytes = bos.toByteArray();
        ByteArrayResource resource = new ByteArrayResource(bytes);
        
        // 4. 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales_chart.xlsx");
        
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(bytes.length)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }
}
