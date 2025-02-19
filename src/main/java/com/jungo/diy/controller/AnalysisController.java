package com.jungo.diy.controller;

import com.jungo.diy.service.AnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

/**
 * @author lichuang3
 * @date 2025-02-19 10:18
 */
@RestController
@Slf4j
@RequestMapping("/analysis")
public class AnalysisController {

    @Autowired
    AnalysisService analysisService;

    // 获取某一接口几号到几号的99线变化曲线
    @GetMapping("/get99LineCurve")
    public String get99LineCurve(@RequestParam("url") String url,
                                 @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                 @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                 HttpServletResponse response) {
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=performance_chart.xlsx");
        return analysisService.get99LineCurve(url, startDate, endDate, response);
    }
}
