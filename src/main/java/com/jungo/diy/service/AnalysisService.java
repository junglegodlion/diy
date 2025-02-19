package com.jungo.diy.service;

import com.jungo.diy.entity.ApiDailyPerformanceEntity;
import com.jungo.diy.mapper.ApiDailyPerformanceMapper;
import com.jungo.diy.model.P99Model;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.jungo.diy.controller.FileReadController.createP99ModelSheet;

/**
 * @author lichuang3
 * @date 2025-02-19 10:21
 */
@Service
@Slf4j
public class AnalysisService {
    @Autowired
    private ApiDailyPerformanceMapper apiDailyPerformanceMapper;

    public String get99LineCurve(String url, LocalDate startDate, LocalDate endDate, HttpServletResponse response) {
        List<ApiDailyPerformanceEntity> apiDailyPerformanceEntities = apiDailyPerformanceMapper.findUrl99Line(url, startDate, endDate);
        // apiDailyPerformanceEntities按照日期排序
        apiDailyPerformanceEntities.sort(Comparator.comparing(ApiDailyPerformanceEntity::getDate));

        List<P99Model> p99Models = getP99Models(apiDailyPerformanceEntities);
        // 画图
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // 定义 Sheet 名称和数据列表
            String[] sheetNames = {"99线", "周维度99线", "慢请求率", "周维度慢请求率"};

            createP99ModelSheet(workbook, "99线变化率", p99Models, "gateway 99线", "日期", "99线", "99线");
            // 6. 保存文件
            workbook.write(response.getOutputStream());
        } catch (Exception e) {
            log.error("AnalysisService#get99LineCurve,出现异常！", e);
        }
        return "success";
    }

    private List<P99Model> getP99Models(List<ApiDailyPerformanceEntity> apiDailyPerformanceEntities) {
        List<P99Model> p99Models = new ArrayList<>();
        for (ApiDailyPerformanceEntity apiDailyPerformanceEntity : apiDailyPerformanceEntities) {
            P99Model p99Model = new P99Model();
            p99Model.setDate(apiDailyPerformanceEntity.getDate().toString());
            p99Model.setP99(apiDailyPerformanceEntity.getP99());
            p99Models.add(p99Model);
        }
        return p99Models;
    }
}
