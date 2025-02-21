package com.jungo.diy.model;

import lombok.Data;

import java.util.List;

/**
 * @author lichuang3
 * @date 2025-02-20 19:57
 */
@Data
public class FolderModel {
    private String folderName;
    private List<FileModel> files;
}
