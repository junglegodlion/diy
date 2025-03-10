package com.jungo.diy.controller;

import com.jungo.diy.service.ChartService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class ChartController {
    
    private final ChartService chartService;

    public ChartController(ChartService chartService) {
        this.chartService  = chartService;
    }

    @GetMapping("/generate-chart")
    public String generateChart() throws IOException {
        chartService.generateLineChart(); 
        return "图表已生成至resources/charts目录";
    }
}