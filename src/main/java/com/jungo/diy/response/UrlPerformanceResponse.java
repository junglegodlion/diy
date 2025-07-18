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
    /**
     * 页面名称
     */
    private String pageName;
    // url
    private String url;
    // 上周99线
    private Integer lastWeekP99;
    // 本周99线
    private Integer thisWeekP99;
    // 上周90线
    private Integer lastWeekP90;
    // 本周90线
    private Integer thisWeekP90;
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
    // 99线目标值
    private Integer p99Target;
    // 是否达标
    private Boolean reachTarget;

    /**
     * 接口类型(1-默认类型 关键路径)
     */
    private Integer interfaceType;

    /**
     * 接口负责人
     */
    private String owner;

}
