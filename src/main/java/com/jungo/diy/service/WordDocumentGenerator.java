package com.jungo.diy.service;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author lichuang3
 */
public class WordDocumentGenerator {

    public static void generateWordDocument(String filePath) throws IOException, InvalidFormatException {
        // 创建一个新的Word文档
        XWPFDocument document = new XWPFDocument();

        // 创建段落
        XWPFParagraph paragraph = document.createParagraph();
        // 创建文本运行
        XWPFRun run = paragraph.createRun();
        run.setText("这是一个使用Apache POI生成的Word文档。");
        // 创建图片段落
        XWPFParagraph imageParagraph = document.createParagraph();
        imageParagraph.setAlignment(ParagraphAlignment.CENTER);

        XWPFRun imageRun = imageParagraph.createRun();
        try (InputStream inputStream = WordDocumentGenerator.class.getResourceAsStream("/images/img.png")) {
            // 包装成支持mark/reset的缓冲流
            BufferedInputStream bufferedStream = new BufferedInputStream(inputStream);
            // 关键点：设置标记
            bufferedStream.mark(Integer.MAX_VALUE);

            BufferedImage originalImage = ImageIO.read(bufferedStream);
            bufferedStream.reset();  // 关键点：重置到标记位置

            imageRun.addPicture(
                    bufferedStream,
                    XWPFDocument.PICTURE_TYPE_PNG,
                    "img.png",
                    Units.toEMU(originalImage.getWidth() / 72f * 2.54)*6,
                    Units.toEMU(originalImage.getHeight() / 72f * 2.54)*6
            );
            System.out.println("原始尺寸：" + originalImage.getWidth() + "x" + originalImage.getHeight());
        } catch (IOException e) {
            throw new RuntimeException("图片加载失败", e);
        }

        // 插入表格前创建段落
        XWPFParagraph tableParagraph = document.createParagraph();
        tableParagraph.createRun().addBreak(); // 添加空行分隔

        XWPFTable table = document.createTable(3, 3);

        // 设置表格宽度（占页面宽度的100%）
        table.setWidth("100%");

        // 设置表格边框（必须）
        CTTblBorders borders = table.getCTTbl().addNewTblPr().addNewTblBorders();
        borders.addNewBottom().setVal(STBorder.SINGLE);
        borders.addNewLeft().setVal(STBorder.SINGLE);
        borders.addNewRight().setVal(STBorder.SINGLE);
        borders.addNewTop().setVal(STBorder.SINGLE);
        borders.addNewInsideH().setVal(STBorder.SINGLE);
        borders.addNewInsideV().setVal(STBorder.SINGLE);

        // 填充单元格内容
        for (int i = 0; i < 3; i++) {
            XWPFTableRow row = table.getRow(i);
            for (int j = 0; j < 3; j++) {
                XWPFTableCell cell = row.getCell(j);
                cell.setText("Cell " + (i+1) + "-" + (j+1));

                // 设置单元格垂直居中
                cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            }
        }



        // 保存文档
        try (FileOutputStream out = new FileOutputStream(filePath)) {
            document.write(out);
        }

    }
}