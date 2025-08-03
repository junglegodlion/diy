package com.jungo.diy.controller;

import com.jungo.diy.service.MaintCriticalDownStreamService;
import com.jungo.diy.util.CsvUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * @author lichuang3
 * @date 2025-08-03 13:28
 */
@Api(tags = "保养关键下游接口性能")
@RestController
@Slf4j
@RequestMapping("/downStream")
public class MaintCriticalDownStreamController {

    @Autowired
    private MaintCriticalDownStreamService maintCriticalDownstreamService;

    @ApiOperation("从文件中读取接口数据")
    @PostMapping("/upload")
    public ResponseEntity<?> uploadInterfaceData(@RequestParam("file") MultipartFile file) {
        try {
            // 获取MultipartFile的名字
            String fileName = file.getOriginalFilename();
            // 从“linklog_linklog_server_side.D2025-08-03T04.01.18” 得到 “2025-08-03”
            String startDate = fileName.substring(fileName.indexOf("D") + 1, fileName.indexOf("T"));
            // “2025-08-03”得到“2025-08-02”
            LocalDate date = LocalDate.parse(startDate);
            LocalDate previousDate = date.minusDays(1);


            List<List<String>> csvData = CsvUtils.getLists(file, true);
            int count = maintCriticalDownstreamService.batchSave(csvData, previousDate);
            return ResponseEntity.ok("数据导入成功，共导入" + count + "条记录");
        } catch (Exception e) {
            log.error("导入接口数据失败", e);
            return ResponseEntity.ok("数据导入失败: " + e.getMessage());
        }
    }
}
