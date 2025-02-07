package com.jungo.diy.model;

import lombok.Data;

/**
 * @author lichuang3
 * @date 2025-02-06 14:13
 */
@Data
public class InterfacePerformanceModel {
    // 唯一标识
    private String token;
    // host
    private String host;
    // url
    private String url;
    // 99线
    private Integer p99;
    // 总调用数量
    private Integer totalRequestCount;
    // 慢请求数
    private Integer slowRequestCount;
    // 慢请求率
    private float slowRequestRate;


    public float getSlowRequestRate() {
        if (totalRequestCount == null || slowRequestCount == null) {
            // 或者抛出异常，根据业务逻辑决定
            return 0.0f;
        }
        return (float) slowRequestCount / totalRequestCount;
    }
}
