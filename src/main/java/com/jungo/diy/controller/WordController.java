package com.jungo.diy.controller;

import com.jungo.diy.service.WordDocumentGenerator;
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
public class WordController {

    @Autowired
    private WordDocumentGenerator WordDocumentGenerator;
    @GetMapping("/generate-word")
    public String generateWord(@RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                               @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        String filePath = "C:\\Users\\lichuang3\\Desktop\\test.docx";
        try {
            WordDocumentGenerator.generateWordDocument(filePath, startDate, endDate);
            return "Word文档生成成功，路径：" + filePath;
        } catch (IOException e) {
            e.printStackTrace();
            return "Word文档生成失败：" + e.getMessage();
        } catch (InvalidFormatException e) {
            throw new RuntimeException(e);
        }
    }
}