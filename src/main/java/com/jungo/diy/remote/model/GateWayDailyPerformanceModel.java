package com.jungo.diy.remote.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author lichuang3
 * @date 2025-06-27 16:39
 */
@Data
public class GateWayDailyPerformanceModel implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("接口请求主机名，支持按主机区分性能数据")
    private String host;

    @ApiModelProperty("接口响应时间第99百分位，衡量性能波动")
    private Integer p99;

    @ApiModelProperty("接口响应时间第99.9百分位，反映极端慢请求")
    private Integer p999;

    @ApiModelProperty("接口响应时间第90百分位，衡量较大请求延迟")
    private Integer p90;

    @ApiModelProperty("接口响应时间第75百分位，用于分析中等延迟请求")
    private Integer p75;

    @ApiModelProperty("接口响应时间第50百分位，表示中位数响应时间")
    private Integer p50;

    @ApiModelProperty("该接口当天总请求次数，用于流量分析")
    private Integer totalRequestCount;

    @ApiModelProperty("该接口当天慢请求次数，慢请求根据内部阈值定义")
    private Integer slowRequestCount;

    @ApiModelProperty("性能统计对应日期，格式为YYYY-MM-DD")
    private Date date;
}
