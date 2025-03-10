package com.jungo.diy.service;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils; 
import org.jfree.chart.JFreeChart; 
import org.jfree.data.category.DefaultCategoryDataset; 
import org.springframework.core.io.ClassPathResource; 
import org.springframework.stereotype.Service; 
import java.io.File; 
import java.io.IOException; 

@Service
public class ChartService {
    
    // 生成折线图并保存到resources/charts目录
    public void generateLineChart() throws IOException {
        // 1. 创建数据集
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(120,  "Sales", "Q1");
        dataset.addValue(240,  "Sales", "Q2");
        dataset.addValue(180,  "Sales", "Q3");
        dataset.addValue(300,  "Sales", "Q4");

        // 2. 创建图表对象
        JFreeChart chart = ChartFactory.createLineChart( 
                "季度销售统计",  // 标题
                "季度",        // X轴标签
                "销售额(万)",  // Y轴标签
                dataset
        );

        // 3. 优化路径处理（新增校验逻辑）
        File outputDir = new ClassPathResource("").getFile().toPath().resolve("charts").toFile();
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create charts directory");
        }

        // 基准宽度
        int baseWidth = 1600;
        // 基准高度
        int baseHeight = 1200;
        // 缩放系数
        float scaleFactor = 1.5f;

        ChartUtils.saveChartAsPNG(
                new File(outputDir, "sales_chart.png"),
                chart,
                (int)(baseWidth * scaleFactor),
                (int)(baseHeight * scaleFactor)
        );
    }
}