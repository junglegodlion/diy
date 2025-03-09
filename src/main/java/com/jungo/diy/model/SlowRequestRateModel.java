package com.jungo.diy.model;

import lombok.Data;

/**
 * @author lichuang3
 * @date 2025-02-11 10:16
 */
@Data
public class SlowRequestRateModel {
    // 日期
    private String date;
    // 周期
    private Integer period;
    // 慢请求率
    private double slowRequestRate;
    // 月份
    private Integer month;
}
