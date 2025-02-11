package com.jungo.diy.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.*;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import java.io.IOException;
import java.io.OutputStream;

public class ExcelChartGenerator {



    public static XDDFCategoryAxis getChartAxis(XSSFChart chart, String title) {
        XDDFCategoryAxis categoryAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        categoryAxis.setTitle(title);
        return categoryAxis;
    }

    public static XDDFValueAxis getValueAxis(XSSFChart chart, String title) {
        XDDFValueAxis valueAxis = chart.createValueAxis(AxisPosition.LEFT);
        valueAxis.setTitle(title);
        valueAxis.setCrosses(AxisCrosses.AUTO_ZERO);
        return valueAxis;
    }

    public static void setLineStyle(XDDFChartData.Series series, PresetColor color) {
        XDDFLineProperties lineProps = new XDDFLineProperties();
        lineProps.setWidth(1.5);
        // lineProps.setDashType(DashType.SOLID);

        XDDFSolidFillProperties fill = new XDDFSolidFillProperties(
            XDDFColor.from(color)
        );
        XDDFShapeProperties shape = new XDDFShapeProperties();
        shape.setFillProperties(fill);
        shape.setLineProperties(lineProps);

        series.setShapeProperties(shape);
    }
}