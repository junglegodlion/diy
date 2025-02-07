package com.jungo.diy.response;

import lombok.Data;

/**
 * @author lichuang3
 * @date 2025-02-07 12:59
 */
@Data
public class UrlPerformanceResponse {

    // host
    private String host;
    // url
    private String url;
    // 上周99线
    private Integer lastWeekP99;
    // 本周99线
    private Integer thisWeekP99;
    // 上周调用量
    private Integer lastWeekTotalRequestCount;
    // 本周调用量
    private Integer thisWeekTotalRequestCount;
    // 上周慢请求率
    private float lastWeekSlowRequestRate;
    // 本周慢请求率
    private float thisWeekSlowRequestRate;
    // 99线变化
    private Integer p99Change;
    // 99线环比
    private float p99ChangeRate;

}
