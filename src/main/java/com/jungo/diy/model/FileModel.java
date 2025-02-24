package com.jungo.diy.model;

import lombok.Data;

import java.util.List;

/**
 * @author lichuang3
 * @date 2025-02-20 19:58
 */
@Data
public class FileModel {
    private String fileName;
    private String data;
    private List<List<String>> dataList;
}
