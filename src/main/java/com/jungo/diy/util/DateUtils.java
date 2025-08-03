package com.jungo.diy.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Calendar;
import java.util.Date;

/**
 * 日期工具类，提供常用的日期格式化和转换方法
 * @author lichuang3
 * @date 2025-03-09 22:15
 */
public class DateUtils {

    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static final String MM_DD = "MM-dd";
    public static final String YYYY_MM = "yyyy-MM";

    private DateUtils() {}

    /**
     * 将LocalDate格式化为指定格式的字符串
     * @param currentDate 要格式化的日期
     * @param format 目标格式
     * @return 格式化后的日期字符串
     */
    public static String getDateString(LocalDate currentDate, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return currentDate.format(formatter);
    }

    /**
     * 获取Date对象对应的月份(1-12)
     * @param date 日期对象
     * @return 月份(1-12)
     */
    public static int getMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MONTH) + 1;
    }

    /**
     * 获取LocalDate对应的ISO周数
     * @param localDate 日期
     * @return ISO标准周数
     */
    public static int getWeekNumber(LocalDate localDate) {
        // 使用ISO周规则计算周数
        // 使用ISO周规则计算周数(周一为一周的第一天)
        WeekFields weekFields = WeekFields.ISO;
        return localDate.get(weekFields.weekOfWeekBasedYear());
    }

    /**
     * 将Date对象格式化为指定格式的字符串
     * @param date 要格式化的日期
     * @param format 目标格式
     * @return 格式化后的日期字符串
     */
    public static String getDateString(Date date, String format) {
        LocalDate localDate = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return localDate.format(DateTimeFormatter.ofPattern(format));
    }

    /**
     * 将字符串解析为LocalDate对象
     * @param date 日期字符串
     * @param format 日期格式
     * @return 解析后的LocalDate对象
     */
    public static LocalDate getLocalDate(String date, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return LocalDate.parse(date, formatter);
    }


    /**
     * 将LocalDate转换为Date对象
     * @param localDate 需要转换的LocalDate对象
     * @return 转换后的Date对象，表示该日期的开始时刻(00:00:00)
     * @throws IllegalArgumentException 当localDate为null时抛出
     */
    public static Date getDate(LocalDate localDate) {
        if (localDate == null) {
            throw new IllegalArgumentException("localDate cannot be null");
        }
        return Date.from(localDate.atStartOfDay(ZoneId.of("UTC")).toInstant());
    }
}
