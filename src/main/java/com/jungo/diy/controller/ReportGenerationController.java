package com.jungo.diy.controller;

import com.jungo.diy.service.ReportGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.time.LocalDate;

/**
 * @author lichuang3
 */
@RestController
@Slf4j
public class ReportGenerationController {

    @Autowired
    private ReportGenerationService reportGenerationService;
    @GetMapping("/generate-word")
    public String generateWord(@RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                               @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            String filePath = reportGenerationService.generateWordDocument(startDate, endDate);
            return "Word文档生成成功，路径：" + filePath;
        } catch (IOException e) {
            log.error("WordController#generateWord,出现异常！", e);
            return "Word文档生成失败：" + e.getMessage();
        } catch (InvalidFormatException e) {
            throw new RuntimeException(e);
        }
    }
}