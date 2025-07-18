package com.jungo.diy.controller;

import com.jungo.diy.constants.FileConstants;
import com.jungo.diy.model.BusinessStatusErrorModel;
import com.jungo.diy.model.UrlStatusErrorModel;
import com.jungo.diy.service.FileService;
import com.jungo.diy.util.FileUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * @author lichuang3
 * @date 2025-07-18 16:51
 */
@Api(tags = "接口成功率统计")
@RestController
@Slf4j
@RequestMapping("/urlSuccessRate")
public class UrlSuccessRateController {

    @Autowired
    private FileService fileService;
    @ApiOperation(value = "获取接口成功率数据")
    @PostMapping("/obtainSuccessRateData")
    public ResponseEntity<String> obtainSuccessRateData(@ApiParam(value = "accesslog", required = true)
                                                        @RequestParam("accesslogFile") MultipartFile accesslogFile,

                                                        @ApiParam(value = "code", required = true)
                                                        @RequestParam("codeFile") MultipartFile codeFile,

                                                        @ApiParam(value = "以yyyy-MM-dd格式表示的日期", required = true)
                                                        @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) throws IOException {
        try {
            FileUtils.ensureDirectoryExists(FileConstants.OUTPUT_DIRECTORY);

            List<UrlStatusErrorModel> statusModels = fileService.processAccessLogFile(accesslogFile);
            List<BusinessStatusErrorModel> businessModels = fileService.processCodeFile(codeFile, date);


            return ResponseEntity.ok("文件处理成功");
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("文件处理失败: " + e.getMessage());
        }
    }
}
