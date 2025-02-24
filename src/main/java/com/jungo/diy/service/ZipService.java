package com.jungo.diy.service;

import com.jungo.diy.model.FileModel;
import com.jungo.diy.model.FolderModel;
import com.jungo.diy.model.PerformanceFileModel;
import com.jungo.diy.model.PerformanceFolderModel;
import com.jungo.diy.util.CsvUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author lichuang3
 * @date 2025-02-21 17:05
 */
@Service
@Slf4j
public class ZipService {
    @Autowired
    FileReaderService fileReaderService;
    public void write2DB(Map<String, FolderModel> folderModelMap) {

        for (Map.Entry<String, FolderModel> entry : folderModelMap.entrySet()) {
            FolderModel folderModel = entry.getValue();
            String folderName = folderModel.getFolderName();
            PerformanceFolderModel performanceFolderModel = new PerformanceFolderModel();
            performanceFolderModel.setFolderName(folderName);
            List<PerformanceFileModel> files = new ArrayList<>();
            performanceFolderModel.setFiles(files);

            // 获取文件夹下的所有文件
            for (FileModel fileModel : folderModel.getFiles()) {
                String fileName = fileModel.getFileName();
                // 将文件内容写入数据库
                PerformanceFileModel performanceFileModel = new PerformanceFileModel();
                performanceFileModel.setFileName(fileName);
                List<List<String>> content = fileModel.getDataList();
                performanceFileModel.setData(content);
                files.add(performanceFileModel);
            }

            try {
                fileReaderService.writeDataToDatabase(performanceFolderModel);
                log.info("ZipService#write2DB,文件夹【{}】写入成功", folderName);
            } catch (Exception e) {
                log.error("ZipService#write2DB,出现异常！文件夹【{}】写入失败", folderName, e);
            }
        }
    }
}
