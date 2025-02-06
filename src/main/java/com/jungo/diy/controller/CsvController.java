package com.jungo.diy.controller;

import com.jungo.diy.util.CsvUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
public class CsvController {

    // 有表头：返回 List<Map>
    @PostMapping("/upload/csv-with-header")
    public List<Map<String, String>> uploadCsvWithHeader(@RequestParam("file") MultipartFile file) throws IOException {
        return CsvUtils.parseCsvWithHeader(file);
    }

    // 无表头：返回 List<List>
    @PostMapping("/upload/csv-raw")
    public List<List<String>> uploadCsvRaw(@RequestParam("file") MultipartFile file) throws IOException {
        return CsvUtils.parseCsvWithoutHeader(file);
    }
}