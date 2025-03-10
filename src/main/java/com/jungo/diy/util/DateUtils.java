package com.jungo.diy.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Calendar;
import java.util.Date;

/**
 * @author lichuang3
 * @date 2025-03-09 22:15
 */
public class DateUtils {

    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static final String MM_DD = "MM-dd";
    public static final String YYYY_MM = "yyyy-MM";

    private DateUtils() {}

    public static String getDateString(LocalDate currentDate, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return currentDate.format(formatter);
    }

    public static int getMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MONTH) + 1;
    }

    public static int getWeekNumber(LocalDate localDate) {
        // 使用ISO周规则计算周数
        WeekFields weekFields = WeekFields.ISO;
        return localDate.get(weekFields.weekOfWeekBasedYear());
    }

    public static String getDateString(Date date, String format) {
        LocalDate localDate = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return localDate.format(DateTimeFormatter.ofPattern(format));
    }


}
