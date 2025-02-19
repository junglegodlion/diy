package com.jungo.diy.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author lichuang3
 */
@RestController
public class ZipController {

    /**
     * 处理 ZIP 文件读取
     *
     * @param file 上传的 ZIP 文件
     * @return 解压结果信息
     */
    @PostMapping("/read-zip")
    public String readZip(@RequestParam("file") MultipartFile file) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream())) {
            processZipEntries(zipInputStream);
            return "Zip processed successfully";
        }
    }

    private void processZipEntries(ZipInputStream zipInputStream) throws IOException {
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                // 处理单个文件（示例：读取文本内容）
                String content = readEntryContent(zipInputStream);
                System.out.println("File:  " + entry.getName() + " | Content: " + content);
            }
            zipInputStream.closeEntry();
        }
    }

    private String readEntryContent(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = zis.read(buffer)) > 0) {
            bos.write(buffer, 0, len);
        }
        return bos.toString(StandardCharsets.UTF_8.name());
    }

    /**
     * 处理 ZIP 文件上传并解压
     *
     * @param file 上传的 ZIP 文件
     * @return 解压结果信息
     */
    @PostMapping("/upload-zip")
    public ResponseEntity<String> handleZipUpload(@RequestParam("file") MultipartFile file) {
        try {
            // 检查文件是否为空
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("上传的文件为空");
            }

            // 检查文件扩展名是否为 .zip
            String fileName = file.getOriginalFilename();
            if (StringUtils.isBlank(fileName) || !fileName.endsWith(".zip")) {
                return ResponseEntity.badRequest().body("请上传有效的 ZIP 文件");
            }

            // 获得当前目录
            String currentDirectory = System.getProperty("user.dir");
            // 创建解压目录
            String outputFolder = currentDirectory + "/" + System.currentTimeMillis();
            Files.createDirectories(Paths.get(outputFolder));

            // 使用 GBK 编码处理 ZIP 文件
            try (ZipInputStream zis = new ZipInputStream(file.getInputStream(), Charset.forName("GBK"))) {
                // 遍历压缩包条目
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        String filePath = outputFolder + "/" + entry.getName();
                        // 调用解压方法
                        extractFile(zis, filePath);
                    }
                    zis.closeEntry();
                }
                return ResponseEntity.ok("解压完成，文件保存至：" + outputFolder);
            }
        } catch (IOException e) {
            return ResponseEntity.status(500).body("解压失败：" + e.getMessage());
        }
    }

    // 文件提取方法（工具类可独立封装）
    private void extractFile(ZipInputStream zis, String filePath) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream(filePath))) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = zis.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
        }
    }
}