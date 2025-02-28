package com.jungo.diy.controller;

import com.jungo.diy.service.WordDocumentGenerator;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;

@RestController
public class WordController {

    @GetMapping("/generate-word")
    public String generateWord() {
        String filePath = "C:\\Users\\lichuang3\\Desktop\\test.docx";
        try {
            WordDocumentGenerator.generateWordDocument(filePath);
            return "Word文档生成成功，路径：" + filePath;
        } catch (IOException e) {
            e.printStackTrace();
            return "Word文档生成失败：" + e.getMessage();
        } catch (InvalidFormatException e) {
            throw new RuntimeException(e);
        }
    }
}