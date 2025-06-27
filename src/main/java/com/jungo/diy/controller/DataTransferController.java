package com.jungo.diy.controller;

/**
 * @author lichuang3
 * @date 2025-06-25 20:28
 */

import com.jungo.diy.service.DataTransferService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author lichuang3
 */
@Api(tags = "数据传输生成控制器")
@RestController
@Slf4j
@RequestMapping("/dataTransfer")
public class DataTransferController {
    @Autowired
    private DataTransferService dataTransferService;

    @ApiOperation(value = "传输核心接口配置数据", notes = "将核心接口配置数据从源系统同步到目标系统")
    @ApiResponses({
            @ApiResponse(code = 200, message = "操作成功", response = Boolean.class),
            @ApiResponse(code = 500, message = "系统内部错误")
    })
    @GetMapping("/transferCoreInterfaceConfig")
    public Boolean transferCoreInterfaceConfig() {
        return dataTransferService.transferCoreInterfaceConfig();
    }

    @ApiOperation(value = "传输API日常性能数据", notes = "将API日常性能数据从源系统同步到目标系统")
    @ApiResponses({
            @ApiResponse(code = 200, message = "操作成功", response = Boolean.class),
            @ApiResponse(code = 500, message = "系统内部错误")
    })
    @GetMapping("/transferApiDailyPerformance")
    public Boolean transferApiDailyPerformance() {
        return dataTransferService.transferApiDailyPerformance();
    }
    
}
