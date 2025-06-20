package com.jungo.diy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author lichuang3
 * @date 2025-02-17 19:07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiDailyPerformanceEntity {
    // id
    private Integer id;
    // host
    private String host;
    // url
    private String url;
    // 999线
    private Integer p999;
    // 99线
    private Integer p99;

    // 90线
    private Integer p90;
    // 75线
    private Integer p75;
    // 50线
    private Integer p50;
    // 95线
    private Integer p95;
    // 总调用数量
    private Integer totalRequestCount;
    // 慢请求数
    private Integer slowRequestCount;
    // 日期
    private Date date;
}
