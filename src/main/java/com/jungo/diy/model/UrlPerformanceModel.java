package com.jungo.diy.model;

import lombok.Data;

/**
 * @author lichuang3
 * @date 2025-02-07 10:41
 */
@Data
public class UrlPerformanceModel {
    // 唯一标识
    private String token;
    // host
    private String host;
    // url
    private String url;
    // 上周的接口性能
    private InterfacePerformanceModel lastWeek;
    // 本周的接口性能
    private InterfacePerformanceModel thisWeek;
    // 99线变化
    private Integer p99Change;
    // 99线环比
    private float p99ChangeRate;

    public float getP99ChangeRate() {
        if (thisWeek.getP99() == null || lastWeek.getP99() == null) {
            return 0f;
        }
        return (float) (thisWeek.getP99() - lastWeek.getP99()) / lastWeek.getP99();
    }

    public Integer getP99Change() {
        if (thisWeek.getP99() == null || lastWeek.getP99() == null) {
            return 0;
        }
        return thisWeek.getP99() - lastWeek.getP99();
    }
}
