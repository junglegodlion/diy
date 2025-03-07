package com.jungo.diy.service;

import com.jungo.diy.enums.InterfaceTypeEnum;
import com.jungo.diy.model.UrlPerformanceModel;
import com.jungo.diy.repository.PerformanceRepository;
import com.jungo.diy.response.UrlPerformanceResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * @author lichuang3
 */
@Service
@Slf4j
public class WordDocumentGenerator {

    @Autowired
    private PerformanceRepository performanceRepository;

    public void generateWordDocument(String filePath,
                                     LocalDate startDate,
                                     LocalDate endDate) throws IOException, InvalidFormatException {

        Map<String, UrlPerformanceModel> urlPerformanceModelMap = performanceRepository.getUrlPerformanceModelMap(startDate, endDate);
        // 关键链路
        List<UrlPerformanceResponse> criticalLinkUrlPerformanceResponses = performanceRepository.getUrlPerformanceResponses(InterfaceTypeEnum.CRITICAL_LINK.getCode(), urlPerformanceModelMap);

        // 创建一个新的Word文档
        try (XWPFDocument document = new XWPFDocument()) {

            // 核心接口监控接口
            setTitle(document, "核心接口监控接口");
            setText(document, "一、关键路径");
            // 获取关键路径的接口数据

            setTable(document);
            setImage(document);
            setTable(document);
            // 保存文档
            try (FileOutputStream out = new FileOutputStream(filePath)) {
                document.write(out);
            }
        }

    }

    private static void setTable(XWPFDocument document) {
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
                cell.setText("Cell " + (i + 1) + "-" + (j + 1));

                // 设置单元格垂直居中
                cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            }
        }
    }

    private static void setImage(XWPFDocument document) throws InvalidFormatException {
        // 创建图片段落
        XWPFParagraph imageParagraph = document.createParagraph();
        imageParagraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun imageRun = imageParagraph.createRun();
        try (InputStream inputStream = WordDocumentGenerator.class.getResourceAsStream("/images/img.png")) {
            if (inputStream == null) {
                throw new IOException("图片资源未找到：/images/img.png");
            }
            BufferedInputStream bufferedStream = new BufferedInputStream(inputStream);
            // 关键点：设置标记
            bufferedStream.mark(Integer.MAX_VALUE);

            BufferedImage originalImage = ImageIO.read(bufferedStream);
            bufferedStream.reset();  // 关键点：重置到标记位置

            imageRun.addPicture(
                    bufferedStream,
                    XWPFDocument.PICTURE_TYPE_PNG,
                    "img.png",
                    Units.toEMU(originalImage.getWidth() / 72f * 2.54) * 6,
                    Units.toEMU(originalImage.getHeight() / 72f * 2.54) * 6
            );
            System.out.println("原始尺寸：" + originalImage.getWidth() + "x" + originalImage.getHeight());
        } catch (IOException e) {
            throw new RuntimeException("图片加载失败", e);
        }
    }

    private static void setText(XWPFDocument document, String text) {
        // 创建段落
        XWPFParagraph paragraph = document.createParagraph();
        // 创建文本运行
        XWPFRun run = paragraph.createRun();
        run.setText(text);
    }

    private static void setTitle(XWPFDocument document, String title) {
        // ▼▼▼▼▼▼▼▼▼▼ 新增标题代码 ▼▼▼▼▼▼▼▼▼▼
        XWPFParagraph titleParagraph = document.createParagraph();
        titleParagraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = titleParagraph.createRun();
        titleRun.setText(title);
        titleRun.setBold(true);
        titleRun.setFontSize(22);
        titleRun.addBreak(BreakType.TEXT_WRAPPING);
        // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲
    }
}