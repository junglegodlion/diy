package com.jungo.diy.controller;

/**
 * @author lichuang3
 * @date 2025-06-25 20:28
 */

import com.jungo.diy.service.DataTransferService;
import io.swagger.annotations.Api;
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

    @GetMapping("/transferCoreInterfaceConfig")
    public Boolean transferCoreInterfaceConfig() {
        return dataTransferService.transferCoreInterfaceConfig();
    }
    
}
