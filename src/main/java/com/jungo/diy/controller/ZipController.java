package com.jungo.diy.controller;

import com.jungo.diy.model.FileModel;
import com.jungo.diy.model.FolderModel;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * @author lichuang3
 */
@RestController
public class ZipController {


    @PostMapping("/read-level-zip")
    public void readLevelZip(@RequestParam("file") MultipartFile file, @RequestParam("write2DB") Integer write2DB) throws IOException {
        // 创建临时文件并自动清理
        File tempFile = File.createTempFile("upload", ".zip");
        Map<String, FolderModel> folderModelMap = new java.util.HashMap<>();
        try {
            // 将上传文件写入临时文件
            file.transferTo(tempFile);
            try (ZipFile zipFile = new ZipFile(tempFile, Charset.forName("GBK"))) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory()) {
                        processDirectoryFiles(zipFile, entry.getName(), write2DB, folderModelMap);
                    }
                }
            }
            System.out.println(folderModelMap);
        } finally {
            // 确保删除临时文件
            if (!tempFile.delete()) {
                tempFile.deleteOnExit();
            }
        }
    }

    private void processDirectoryFiles(ZipFile zipFile, String dirPrefix, Integer write2DB, Map<String, FolderModel> folderModelMap) throws IOException {
        Enumeration<? extends ZipEntry> allEntries = zipFile.entries();


        while (allEntries.hasMoreElements()) {
            ZipEntry fileEntry = allEntries.nextElement();

            if (!fileEntry.isDirectory() &&
                    fileEntry.getName().startsWith(dirPrefix) &&
                    // 排除目录自身
                    !fileEntry.getName().equals(dirPrefix)) {
                String name = fileEntry.getName();
                // 将name按照"/"分割
                String[] segments = name.split("/");
                String folderName = segments[0];
                String fileName = segments[1];
                FolderModel folderModel = folderModelMap.get(folderName);
                if (folderModel == null) {
                    folderModel = new FolderModel();
                    folderModel.setFolderName(folderName);
                    folderModel.setFiles(new java.util.ArrayList<>());
                    folderModelMap.put(folderName, folderModel);
                }

                try (InputStream is = zipFile.getInputStream(fileEntry)) {
                    String content = IOUtils.toString(is, StandardCharsets.UTF_8);
                    if (write2DB != null && write2DB == 1) {
                        List<FileModel> files = folderModel.getFiles();
                        FileModel fileModel = new FileModel();
                        fileModel.setFileName(fileName);
                        fileModel.setData(content);
                        files.add(fileModel);
                    } else {
                        System.out.println("Subfile: " + fileEntry.getName());
                    }

                }
            }
        }
    }


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

    /**
     * 处理 ZIP 文件条目并输出文件内容
     *
     * 遍历 ZIP 输入流中的所有条目，对非目录文件执行内容读取操作。该方法会循环处理直到 ZIP 流中无更多条目，
     * 自动跳过目录类型的 ZIP 条目，调用者无需手动关闭输入流
     *
     * @param zipInputStream ZIP 文件输入流对象，用于迭代读取 ZIP 条目，要求非空且由调用方管理资源关闭
     * @throws IOException 当读取 ZIP 流发生 I/O 错误时抛出，包括流损坏、权限问题等情况
     */
    private void processZipEntries(ZipInputStream zipInputStream) throws IOException {
        ZipEntry entry;
        // 循环遍历 ZIP 文件中的所有条目直到流结束
        while ((entry = zipInputStream.getNextEntry()) != null) {
            // 跳过目录类型的虚拟条目
            if (!entry.isDirectory()) {
                // 处理单个文件：读取条目内容并输出（示例实现）
                // 实际应用可替换为文件提取、内容分析等自定义处理逻辑
                String content = readEntryContent(zipInputStream);
                System.out.println("File:  " + entry.getName() + " | Content: " + content);
            }
            // 必须显式关闭当前 ZIP 条目才能读取下一个条目
            zipInputStream.closeEntry();
        }
    }

    /**
     * 读取 ZIP 文件条目的内容
     *
     * @param zis ZIP 文件输入流
     * @return 文件内容
     */
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