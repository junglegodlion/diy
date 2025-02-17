package com.jungo.diy.model;

import lombok.Data;

import java.util.List;

/**
 * @author lichuang3
 * @date 2025-02-17 20:40
 */
@Data
public class PerformanceFolderModel {
    private String folderName;
    private List<PerformanceFileModel> files;
}
