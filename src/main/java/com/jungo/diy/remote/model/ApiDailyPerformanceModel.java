package com.jungo.diy.remote.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author lichuang3
 * @date 2025-06-26 17:59
 */
@Data
public class ApiDailyPerformanceModel implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "请求来源主机或IP地址信息，用于区分不同来源", example = "cl-gateway.tuhu.cn")
    private String host;

    @ApiModelProperty(value = "接口请求的完整URL路径，用于性能统计", example = "/api/v1/monitor")
    private String url;

    @ApiModelProperty(value = "接口P99响应时间，单位毫秒，即99%请求的最大响应时间", example = "500")
    private Integer p99;

    @ApiModelProperty(value = "接口P999响应时间，单位毫秒，即99.9%请求的最大响应时间", example = "1000")
    private Integer p999;

    @ApiModelProperty(value = "接口P90响应时间，单位毫秒，即90%请求的最大响应时间", example = "300")
    private Integer p90;

    @ApiModelProperty(value = "接口P75响应时间，单位毫秒，即75%请求的最大响应时间", example = "200")
    private Integer p75;

    @ApiModelProperty(value = "接口P50响应时间，单位毫秒，即50%请求的最大响应时间，中位数", example = "100")
    private Integer p50;

    @ApiModelProperty(value = "P95响应时间(毫秒)，即95%请求的最大响应时间", example = "400")
    private Integer p95;

    @ApiModelProperty(value = "当天该接口累计请求总次数，用于流量统计", example = "10000")
    private Integer totalRequestCount;

    @ApiModelProperty(value = "当天慢请求次数，慢请求定义可根据业务配置，比如>500ms", example = "50")
    private Integer slowRequestCount;

    @ApiModelProperty(value = "统计日期，格式为yyyy-MM-dd", example = "2025-06-26")
    private Date date;
}
