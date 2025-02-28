package com.jungo.diy.service;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xwpf.usermodel.*;
import java.io.FileOutputStream;
import java.io.IOException;

public class WordDocumentGenerator {

    public static void generateWordDocument(String filePath) throws IOException, InvalidFormatException {
        // 创建一个新的Word文档
        XWPFDocument document = new XWPFDocument();

        // 创建段落
        XWPFParagraph paragraph = document.createParagraph();
        // 创建文本运行
        XWPFRun run = paragraph.createRun();
        run.setText("这是一个使用Apache POI生成的Word文档。");


        // 保存文档
        try (FileOutputStream out = new FileOutputStream(filePath)) {
            document.write(out);
        }
    }
}