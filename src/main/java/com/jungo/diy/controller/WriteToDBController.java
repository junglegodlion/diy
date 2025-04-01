
package com.jungo.diy.controller;

import com.jungo.diy.service.FileReaderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lichuang3
 */
@Api(tags = "文件操作控制器")
@RestController
@RequestMapping("/files")
public class WriteToDBController {
    @Autowired
    private FileReaderService fileReaderService;

    // 多文件存储数据
    @ApiOperation(value = "从多个目录获取文件", response = ResponseEntity.class)
    @GetMapping("/getMultiFile")
    public ResponseEntity<?> getFiles(@ApiParam(value = "开始目录名", required = true) @RequestParam("startDirectoryName") String startDirectoryName,
                                      @ApiParam(value = "结束目录名", required = true) @RequestParam("endDirectoryName") String endDirectoryName) {
        return ResponseEntity.ok(fileReaderService.getMultiFile(startDirectoryName, endDirectoryName));
    }

    @ApiOperation(value = "从指定目录获取文件", response = ResponseEntity.class)
    @GetMapping("/getSingleFile")
    public ResponseEntity<?> getSingleFile(@ApiParam(value = "目录名", required = true) @RequestParam("directoryName") String directoryName) {
        return ResponseEntity.ok(fileReaderService.readTargetFiles(directoryName));
    }
}
