package com.jungo.diy.model;

import lombok.Data;

import java.util.List;

/**
 * @author lichuang3
 * @date 2025-02-17 20:43
 */
@Data
public class PerformanceFileModel {
    private String fileName;
    private List<List<String>> data;
}
