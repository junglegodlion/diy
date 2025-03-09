package com.jungo.diy.service;

import com.jungo.diy.enums.InterfaceTypeEnum;
import com.jungo.diy.model.SlowRequestRateModel;
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
import java.math.RoundingMode;
import java.text.DecimalFormat;
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

        List<SlowRequestRateModel> gatewayAverageSlowRequestRate = performanceRepository.getGatewayAverageSlowRequestRate(LocalDate.now().getYear());
        Map<String, UrlPerformanceModel> urlPerformanceModelMap = performanceRepository.getUrlPerformanceModelMap(startDate, endDate);
        // 关键链路
        List<UrlPerformanceResponse> criticalLinkUrlPerformanceResponses = performanceRepository.getUrlPerformanceResponses(InterfaceTypeEnum.CRITICAL_LINK.getCode(), urlPerformanceModelMap);

        // 创建一个新的Word文档
        try (XWPFDocument document = new XWPFDocument()) {
            // 一级标题
            setTitle(document, "慢请求率");
            // 表格是动态的，现在是几月，就要有几行
            drawGatewayMonthlyAverageSlowRequestRateTable(document, gatewayAverageSlowRequestRate);

            // 核心接口监控接口
            setTitle(document, "核心接口监控接口");
            setText(document, "一、关键路径");
            // 获取关键路径的接口数据
            setCriticalLinkTable(document, criticalLinkUrlPerformanceResponses);
            setImage(document);
            setTable(document);
            // 保存文档
            try (FileOutputStream out = new FileOutputStream(filePath)) {
                document.write(out);
            }
        }

    }

    private static void drawGatewayMonthlyAverageSlowRequestRateTable(XWPFDocument document, List<SlowRequestRateModel> gatewayAverageSlowRequestRate) {
        // 获取当前月份
        int currentMonth = LocalDate.now().getMonthValue();
        // 插入表格前创建段落
        XWPFParagraph tableParagraph = document.createParagraph();
        tableParagraph.createRun().addBreak(); // 添加空行分隔

        // 创建表格，行数为当前月份 + 1（表头行），列数为3
        XWPFTable table = document.createTable(2, currentMonth);

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

        // 设置表格表头
        XWPFTableRow headerRow = table.getRow(0);
        int year = LocalDate.now().getYear();
        for (int i = 0; i < currentMonth; i++) {
            String title = year + "-" + (i + 1);
            headerRow.getCell(i).setText(title);
        }

        XWPFTableRow row = table.getRow(1);
        // 填充表格内容
        for (int i = 0; i < currentMonth; i++) {
            double slowRequestRate = gatewayAverageSlowRequestRate.get(i).getSlowRequestRate();
            // slowRequestRate转化是百分数，保留2位小数
            DecimalFormat df = new DecimalFormat("0.00%");
            // 可选：设置四舍五入模式（默认HALF_EVEN）
            df.setRoundingMode(RoundingMode.HALF_UP);
            row.getCell(i).setText(df.format(slowRequestRate));
        }
    }

    public static void main(String[] args) {
        System.out.println(LocalDate.now().getMonthValue());
    }


    private static void setCriticalLinkTable(XWPFDocument document, List<UrlPerformanceResponse> criticalLinkUrlPerformanceResponses) {
        // 插入表格前创建段落
        XWPFParagraph tableParagraph = document.createParagraph();
        tableParagraph.createRun().addBreak(); // 添加空行分隔

        XWPFTable table = document.createTable(criticalLinkUrlPerformanceResponses.size() + 1, 3);

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

        // 设置表格表头
        XWPFTableRow headerRow = table.getRow(0);
        XWPFTableCell pageNameCell = headerRow.getCell(0);
        pageNameCell.setText("pageName");
        pageNameCell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

        // 填充单元格内容
        for (int i = 0; i < criticalLinkUrlPerformanceResponses.size(); i++) {
            XWPFTableRow row = table.getRow(i + 1);
            UrlPerformanceResponse urlPerformanceResponse = criticalLinkUrlPerformanceResponses.get(i);
            XWPFTableCell cell = row.getCell(0);
            cell.setText(urlPerformanceResponse.getPageName());
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
        // 创建段落
        XWPFParagraph titleParagraph = document.createParagraph();
        // 设置为“标题1”样式
        titleParagraph.setStyle("Heading1");
        // 设置段落居中对齐
        titleParagraph.setAlignment(ParagraphAlignment.CENTER);
        // 创建文本运行
        XWPFRun titleRun = titleParagraph.createRun();
        titleRun.setText(title);
        // 加粗
        titleRun.setBold(true);
        // 设置字体大小
        titleRun.setFontSize(22);
        // 添加换行
        titleRun.addBreak(BreakType.TEXT_WRAPPING);
    }
}