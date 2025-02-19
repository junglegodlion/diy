package com.jungo.diy.controller;

import com.jungo.diy.service.FileReaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lichuang3
 */
@RestController
@RequestMapping("/files")
public class WriteToDBController {
    @Autowired
    private FileReaderService fileReaderService;
 
    @GetMapping
    public ResponseEntity<?> getFiles(@RequestParam("directoryName") String directoryName) {
        return ResponseEntity.ok(fileReaderService.readTargetFiles(directoryName));
    }
}