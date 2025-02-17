package com.jungo.diy.controller;

import com.jungo.diy.entity.ApiDailyPerformanceEntity;
import com.jungo.diy.service.ApiDailyPerformanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author lichuang3
 * @date 2025-02-17 19:22
 */
@RestController
@RequestMapping("/apiDailyPerformance")
public class ApiDailyPerformanceController {
    @Autowired
    private ApiDailyPerformanceService apiDailyPerformanceService;

    @GetMapping("/listApiDailyPerformanceEntities")
    public List<ApiDailyPerformanceEntity> listApiDailyPerformanceEntities() {
        return apiDailyPerformanceService.getAllApiDailyPerformanceEntities();
    }

    @PostMapping("/createApiDailyPerformance")
    public String createApiDailyPerformance(@RequestBody ApiDailyPerformanceEntity apiDailyPerformance) {
        apiDailyPerformanceService.addApiDailyPerformance(apiDailyPerformance);
        return "success";
    }
}
