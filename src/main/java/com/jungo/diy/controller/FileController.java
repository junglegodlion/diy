package com.jungo.diy.controller;

import com.jungo.diy.service.FileReaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/files")
public class FileController {
    @Autowired
    private FileReaderService fileReaderService;
 
    @GetMapping
    public ResponseEntity<?> getFiles() {
        return ResponseEntity.ok(fileReaderService.readTargetFiles()); 
    }
}