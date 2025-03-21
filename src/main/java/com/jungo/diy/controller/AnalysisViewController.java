package com.jungo.diy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.time.LocalDate;

/**
 * @author lichuang3
 */ // 新建页面专用控制器
@Controller
@RequestMapping("/analysis")
public class AnalysisViewController {
    @GetMapping("/page")
    public String showAnalysisPage(Model model) {
        model.addAttribute("startDate", LocalDate.now().minusDays(7));
        model.addAttribute("endDate", LocalDate.now());
        return "analysis";
    }
}