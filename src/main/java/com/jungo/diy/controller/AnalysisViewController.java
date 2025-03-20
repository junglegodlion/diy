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

    @GetMapping("/download")
    public void downloadFile(
            @RequestParam String filePath,
            HttpServletResponse response) throws IOException {
        File file = new File(filePath);
        response.setContentType("application/msword");
        response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
        Files.copy(file.toPath(), response.getOutputStream());
    }
}