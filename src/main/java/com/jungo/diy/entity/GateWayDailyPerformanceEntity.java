package com.jungo.diy.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.Date;

/**
 * @author lichuang3
 * @date 2025-02-17 19:07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GateWayDailyPerformanceEntity {
    // id
    private Long id;
    // host
    private String host;
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
    // 总调用数量
    private Integer totalRequestCount;
    // 慢请求数
    private Integer slowRequestCount;
    // 日期
    private Date date;

    int weekNumber;

    // 慢请求率
    private double slowRequestRate;

    public double getSlowRequestRate() {
        if (totalRequestCount == null || slowRequestCount == null) {
            // 或者抛出异常，根据业务逻辑决定
            return 0.0f;
        }
        return (float) slowRequestCount / totalRequestCount;
    }

    public int getWeekNumber() {
        LocalDate localDate = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        // 使用ISO周规则计算周数
        WeekFields weekFields = WeekFields.ISO;
        return localDate.get(weekFields.weekOfWeekBasedYear());
    }
}
