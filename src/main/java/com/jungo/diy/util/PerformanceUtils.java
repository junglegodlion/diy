package com.jungo.diy.util;

import com.jungo.diy.model.P99Model;
import com.jungo.diy.model.SlowRequestRateModel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lichuang3
 * @date 2025-02-24 18:15
 */

public class PerformanceUtils {

    public static List<P99Model> getAverageP99Models(List<P99Model> p99Models) {
        // 将p99Models按照周数分组，组内的顺序按照日期排序，并计算平均值
        Map<Integer, List<P99Model>> groupedP99Models = p99Models.stream().collect(Collectors.groupingBy(P99Model::getPeriod));
        List<P99Model> averageP99Models = new ArrayList<>();
        for (Map.Entry<Integer, List<P99Model>> entry : groupedP99Models.entrySet()) {
            List<P99Model> p99ModelList = entry.getValue();
            int sum = p99ModelList.stream().mapToInt(P99Model::getP99).sum();
            int average = sum / p99ModelList.size();
            P99Model averageP99Model = new P99Model();

            // 设置2025年第2周（ISO标准周计算）
            LocalDate date = LocalDate.of(2025, 1, 1)
                    .with(WeekFields.ISO.weekOfYear(), entry.getKey())
                    // 周三
                    .with(WeekFields.ISO.dayOfWeek(), 3);

            averageP99Model.setDate(date.format(DateTimeFormatter.ISO_DATE));
            averageP99Model.setPeriod(entry.getKey());
            averageP99Model.setP99(average);
            averageP99Models.add(averageP99Model);
        }
        return averageP99Models;
    }


    public static List<SlowRequestRateModel> getAverageSlowRequestRateModels(List<SlowRequestRateModel> slowRequestRateModels) {
        // 将slowRequestRateModels按照周数分组，计算平均值
        Map<Integer, List<SlowRequestRateModel>> groupedSlowRequestRateModels = slowRequestRateModels.stream().collect(Collectors.groupingBy(SlowRequestRateModel::getPeriod));
        List<SlowRequestRateModel> averageSlowRequestRateModels = new ArrayList<>();
        for (Map.Entry<Integer, List<SlowRequestRateModel>> entry : groupedSlowRequestRateModels.entrySet()) {
            List<SlowRequestRateModel> slowRequestRateModelList = entry.getValue();

            // 计算平均慢请求率
            double sum = slowRequestRateModelList.stream().mapToDouble(SlowRequestRateModel::getSlowRequestRate).sum();
            double average = sum / slowRequestRateModelList.size();
            SlowRequestRateModel slowRequestRateModel = new SlowRequestRateModel();

            // 设置2025年第2周（ISO标准周计算）
            LocalDate date = LocalDate.of(2025, 1, 1)
                    .with(WeekFields.ISO.weekOfYear(), entry.getKey())
                    .with(WeekFields.ISO.dayOfWeek(), 3); // 周三

            slowRequestRateModel.setDate(date.format(DateTimeFormatter.ISO_DATE));
            slowRequestRateModel.setPeriod(entry.getKey());
            slowRequestRateModel.setSlowRequestRate(average);
            averageSlowRequestRateModels.add(slowRequestRateModel);
        }
        return averageSlowRequestRateModels;
    }
}
