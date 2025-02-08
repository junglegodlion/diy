package com.jungo.diy.controller;

import com.jungo.diy.util.ExcelChartGenerator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/chart")
public class ChartController {

    @GetMapping("/export")
    public void exportChartExcel(HttpServletResponse response) throws IOException {
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=sales_chart.xlsx");
        
        // 生成带图表的 Excel
        ExcelChartGenerator.generateChartWithData(response.getOutputStream());
    }
}