package com.jungo.diy.model;

import lombok.Data;

/**
 * @author lichuang3
 * @date 2025-02-10 19:00
 */
@Data
public class P99Model {

    // 日期
    private String date;
    // 周期
    private Integer period;
    // 99线
    private Integer p99;
}
