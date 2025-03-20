package com.jungo.diy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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