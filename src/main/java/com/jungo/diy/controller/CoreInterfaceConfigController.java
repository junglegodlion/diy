package com.jungo.diy.controller;

import com.jungo.diy.entity.CoreInterfaceConfigEntity;
import com.jungo.diy.model.ExcelModel;
import com.jungo.diy.model.SheetModel;
import com.jungo.diy.request.CoreInterfaceConfigRequest;
import com.jungo.diy.service.CoreInterfaceConfigService;
import com.jungo.diy.service.FileService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * @author lichuang3
 * @date 2025-02-25 19:23
 */
@RestController
@Slf4j
@RequestMapping("/coreInterfaceConfig")
public class CoreInterfaceConfigController {

    @Autowired
    FileService fileService;

    @Autowired
    CoreInterfaceConfigService coreInterfaceConfigService;

    // 新增接口：读取Excel文件中数据，将数据保存到数据库中
    @PostMapping("/upload/getConfig")
    public void readFile(@RequestParam("file") MultipartFile file) throws IOException {
        ExcelModel excelModel = fileService.readXlsxFile(file);
        SheetModel sheetModel = excelModel.getSheetModels().get(0);
        List<List<String>> data = sheetModel.getData();
        coreInterfaceConfigService.write2DB(data);
    }

    @ApiOperation(value = "插入核心接口配置", notes = "将传入的核心接口配置信息插入到数据库中")
    @PostMapping("/insert")
    public String insert(@RequestBody CoreInterfaceConfigRequest request){
        int result = coreInterfaceConfigService.insert(request);
        return result > 0 ? "success" : "fail";
    }

}
