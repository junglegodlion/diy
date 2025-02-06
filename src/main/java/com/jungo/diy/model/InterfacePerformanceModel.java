package com.jungo.diy.model;

import lombok.Data;

/**
 * @author lichuang3
 * @date 2025-02-06 14:13
 */
@Data
public class InterfacePerformanceModel {
    // url
    private String url;
    // 99线
    private String p99;
    // 总调用数量
    private String totalRequestCount;
    // 慢请求数
    private String slowRequestCount;

}
