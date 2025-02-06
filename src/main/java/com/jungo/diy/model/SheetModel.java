package com.jungo.diy.model;

import lombok.Data;

import java.util.List;

/**
 * @author lichuang3
 * @date 2025-02-06 14:43
 */
@Data
public class SheetModel {
    private int sheetIndex;
    private String sheetName;
    private List<List<String>> data;
}
