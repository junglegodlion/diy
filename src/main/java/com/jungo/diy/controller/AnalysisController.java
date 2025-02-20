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
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PastOrPresent;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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

    // 获取网关最近一周性能变化曲线图
    @GetMapping("/getGateWayPerformanceCurve")
    public void getGateWayPerformanceCurve(@RequestParam("year") Integer year,
                                           @RequestParam("month") Integer month,
                                           HttpServletResponse response) {
        analysisService.getGateWayPerformanceCurve(year, month, response);
    }

    // 获取某一接口几号到几号的99线变化曲线
    @GetMapping("/get99LineCurve")
    public String get99LineCurve(@RequestParam("url") @NotBlank(message = "URL不能为空") String url,
                                 @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") @PastOrPresent LocalDate startDate,
                                 @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                 HttpServletResponse response) {
        // 日期范围校验
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=get99LineCurve_chart.xlsx");
        return analysisService.get99LineCurve(url, startDate, endDate, response);
    }

    // 获取某号和某号的核心接口性能对比数据
    @GetMapping("/getCorePerformanceCompare")
    public void getCorePerformanceCompare(@RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                          @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                          HttpServletResponse response) throws UnsupportedEncodingException {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }

        // 限制最大查询跨度（示例：不超过31天）
        if (ChronoUnit.DAYS.between(startDate, endDate) > 31) {
            throw new IllegalArgumentException("查询时间跨度不能超过31天");
        }

        analysisService.getCorePerformanceCompare(startDate, endDate, response);
    }
}
