
package com.jungo.diy.controller;

import com.jungo.diy.config.RequestContext;
import com.jungo.diy.model.P99Model;
import com.jungo.diy.service.AnalysisService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * @author lichuang3
 * @date 2025-02-19 10:18
 */
@RestController
@Slf4j
@RequestMapping("/analysis")
@Api(tags = "分析控制器", description = "API for analysis operations")
public class AnalysisController {

    @Autowired
    AnalysisService analysisService;

    /**
     * 获取网关性能变化曲线图
     *
     * @param year 统计年份，用于指定查询数据的年份范围
     * @param startDate 起始日期（格式：yyyy-MM-dd），要求为当前或过去的日期，用于限定查询数据的时间范围起点
     * @param response HTTP响应对象，用于直接向客户端输出图表数据（如图片流或文件下载）
     */
    @GetMapping("/getGateWayPerformanceCurveChart")
    @ApiOperation(value = "获取网关性能变化曲线图", notes = "通过analysisService生成网关性能曲线图表后，直接通过response对象返回结果")
    public void getGateWayPerformanceCurveChart(
            @ApiParam(value = "统计年份", required = true) @RequestParam("year") Integer year,
            @ApiParam(value = "起始日期（格式：yyyy-MM-dd）", required = true) @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") @PastOrPresent LocalDate startDate,
            HttpServletResponse response) {
        analysisService.getGateWayPerformanceCurveChart(year, startDate, response);
    }

    /**
     * 图-获取某一接口几号到几号的99线变化曲线
     *
     * @param url 接口URL
     * @param startDate 起始日期（格式：yyyy-MM-dd），要求为当前或过去的日期，用于限定查询数据的时间范围起点
     * @param endDate 结束日期（格式：yyyy-MM-dd），要求为当前或过去的日期，用于限定查询数据的时间范围终点
     * @param response HTTP响应对象，用于直接向客户端输出图表数据（如图片流或文件下载）
     */
    @GetMapping("/get99LineCurve")
    @ApiOperation(value = "图-获取某一接口几号到几号的99线变化曲线", notes = "通过analysisService生成99线变化曲线图表后，直接通过response对象返回结果")
    public String get99LineCurve(
            @ApiParam(value = "接口URL", required = true) @RequestParam("url") @NotBlank(message = "URL不能为空") String url,
            @ApiParam(value = "起始日期（格式：yyyy-MM-dd）", required = true) @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") @PastOrPresent LocalDate startDate,
            @ApiParam(value = "结束日期（格式：yyyy-MM-dd）", required = true) @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletResponse response) {
        // 日期范围校验
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }

        // 写入上下文
        RequestContext.put("startDate", startDate);
        RequestContext.put("endDate", endDate);

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=get99LineCurve_chart.xlsx");
        return analysisService.get99LineCurve(url, response);
    }

    /**
     * 数据-获取某一接口几号到几号的99线变化曲线
     *
     * @param url 接口URL
     * @param startDate 起始日期（格式：yyyy-MM-dd），要求为当前或过去的日期，用于限定查询数据的时间范围起点
     * @param endDate 结束日期（格式：yyyy-MM-dd），要求为当前或过去的日期，用于限定查询数据的时间范围终点
     * @param response HTTP响应对象，用于直接向客户端输出图表数据（如图片流或文件下载）
     */
    @GetMapping("/get99LineData")
    @ApiOperation(value = "数据-获取某一接口几号到几号的99线变化")
    public List<P99Model> get99LineData(
            @ApiParam(value = "接口URL", required = true) @RequestParam("url") @NotBlank(message = "URL不能为空") String url,
            @ApiParam(value = "起始日期（格式：yyyy-MM-dd）", required = true) @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") @PastOrPresent LocalDate startDate,
            @ApiParam(value = "结束日期（格式：yyyy-MM-dd）", required = true) @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletResponse response) {
        // 日期范围校验
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }

        // 写入上下文
        RequestContext.put("startDate", startDate);
        RequestContext.put("endDate", endDate);

        // 设置响应头
        return analysisService.get99LineData(url, response);
    }

    /**
     * 批量获取接口几号到几号的99线变化曲线
     *
     * @param urls 接口URL数组
     * @param startDate 起始日期（格式：yyyy-MM-dd），要求为当前或过去的日期，用于限定查询数据的时间范围起点
     * @param endDate 结束日期（格式：yyyy-MM-dd），要求为当前或过去的日期，用于限定查询数据的时间范围终点
     * @param response HTTP响应对象，用于直接向客户端输出图表数据（如图片流或文件下载）
     */
    @GetMapping("/batchGet99LineCurve")
    @ApiOperation(value = "批量获取接口几号到几号的99线变化曲线", notes = "通过analysisService批量生成99线变化曲线图表后，直接通过response对象返回结果")
    public String batchGet99LineCurve(
            @ApiParam(value = "接口URL数组", required = true) @RequestParam("urls") String[] urls,
            @ApiParam(value = "起始日期（格式：yyyy-MM-dd）", required = true) @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") @PastOrPresent LocalDate startDate,
            @ApiParam(value = "结束日期（格式：yyyy-MM-dd）", required = true) @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletResponse response) {
        // 日期范围校验
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=batchGet99LineCurve_chart.xlsx");
        return analysisService.batchGet99LineCurve(urls, startDate, endDate, response);
    }

    /**
     * 批量获取接口慢请求率的变化曲线
     *
     * @param urls 接口URL数组
     * @param startDate 起始日期（格式：yyyy-MM-dd），要求为当前或过去的日期，用于限定查询数据的时间范围起点
     * @param endDate 结束日期（格式：yyyy-MM-dd），要求为当前或过去的日期，用于限定查询数据的时间范围终点
     * @param response HTTP响应对象，用于直接向客户端输出图表数据（如图片流或文件下载）
     */
    @GetMapping("/batchGetSlowRequestRateCurve")
    @ApiOperation(value = "批量获取接口慢请求率的变化曲线", notes = "通过analysisService批量生成慢请求率变化曲线图表后，直接通过response对象返回结果")
    public String batchGetSlowRequestRateCurve(
            @ApiParam(value = "接口URL数组", required = true) @RequestParam("urls") String[] urls,
            @ApiParam(value = "起始日期（格式：yyyy-MM-dd）", required = true) @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") @PastOrPresent LocalDate startDate,
            @ApiParam(value = "结束日期（格式：yyyy-MM-dd）", required = true) @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletResponse response) {
        // 日期范围校验
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=batchGet99LineCurve_chart.xlsx");
        return analysisService.batchGetSlowRequestRateCurve(urls, startDate, endDate, response);
    }

    /**
     * 获取某号和某号的核心接口性能对比数据
     *
     * @param startDate 起始日期（格式：yyyy-MM-dd），要求为当前或过去的日期，用于限定查询数据的时间范围起点
     * @param endDate 结束日期（格式：yyyy-MM-dd），要求为当前或过去的日期，用于限定查询数据的时间范围终点
     * @param response HTTP响应对象，用于直接向客户端输出图表数据（如图片流或文件下载）
     */
    @GetMapping("/getCorePerformanceCompare")
    @ApiOperation(value = "获取某号和某号的核心接口性能对比数据", notes = "通过analysisService生成核心接口性能对比数据后，直接通过response对象返回结果")
    public String getCorePerformanceCompare(
            @ApiParam(value = "起始日期（格式：yyyy-MM-dd）", required = true) @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @ApiParam(value = "结束日期（格式：yyyy-MM-dd）", required = true) @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletResponse response) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }

        // 限制最大查询跨度（示例：不超过31天）
        if (ChronoUnit.DAYS.between(startDate, endDate) > 31) {
            throw new IllegalArgumentException("查询时间跨度不能超过31天");
        }

        return analysisService.getCorePerformanceCompare(startDate, endDate, response);
    }
}
