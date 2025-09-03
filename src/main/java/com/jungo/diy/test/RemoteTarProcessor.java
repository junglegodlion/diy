package com.jungo.diy.test;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Stream;

@Slf4j
public class RemoteTarProcessor {

    // 下载远程 tar 文件到本地
    public static File downloadTar(String url, String savePath) throws IOException {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("URL不能为空");
        }

        if (StringUtils.isBlank(savePath)) {
            throw new IllegalArgumentException("保存路径不能为空");
        }

        log.info("开始下载文件: {}", url);

        // 添加超时设置
        HttpResponse<byte[]> response = Unirest.get(url)
                .header("Accept", "*/*")
                .connectTimeout(30000)
                .socketTimeout(60000)
                .asBytes();

        int statusCode = response.getStatus();
        if (statusCode != 200) {
            String errorMsg = String.format("下载失败，HTTP状态码：%d, URL: %s", statusCode, url);
            log.error(errorMsg);
            throw new IOException(errorMsg);
        }

        File tarFile = new File(savePath);
        // 确保目录存在
        File parentDir = tarFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(tarFile)) {
            fos.write(response.getBody());
        }

        log.info("下载完成，保存路径: {}", tarFile.getAbsolutePath());
        return tarFile;
    }

    // 解压 tar 文件
    public static void extractTarFile(File tarFile, File outputDir) throws IOException {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        try (FileInputStream fis = new FileInputStream(tarFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             TarArchiveInputStream tais = new TarArchiveInputStream(bis)) {

            TarArchiveEntry entry;
            while ((entry = tais.getNextTarEntry()) != null) {
                if (entry.isDirectory()) continue;

                File outFile = new File(outputDir, entry.getName());
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    IOUtils.copy(tais, fos);
                }
                System.out.println("解压完成: " + outFile.getAbsolutePath());
            }
        }
    }

    // 解析文件内容
    public static void parseFile(File file) throws IOException {
        System.out.println("解析文件: " + file.getName());
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    // 自定义解析逻辑
                    System.out.println(line);
                }
            }
        }
    }

    // 删除文件或目录
    public static void deleteFileOrDir(File file) throws IOException {
        if (!file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            try (Stream<Path> paths = Files.walk(file.toPath())) {
                paths.sorted(Comparator.reverseOrder()) // 使用内置比较器
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                throw new UncheckedIOException("删除文件失败: " + path, e);
                            }
                        });
            } catch (UncheckedIOException e) {
                throw e.getCause(); // 重新抛出原始IOException
            }
        } else {
            Files.deleteIfExists(file.toPath());
        }

        System.out.println("已删除: " + file.getAbsolutePath());
    }


    public static void main(String[] args) {
        String url = "https://sre.tuhuyun.cn/upload/log_data_export/2025-09-03/lichuang-wanaccess.tar";
        String tarPath = "lichuang-wanaccess.tar";  // 下载到本地
        String outputDir = "lichuang-wanaccess";    // 解压目录

        try {
            // 1. 下载
            File tarFile = downloadTar(url, tarPath);

            // 2. 解压
            File outDir = new File(outputDir);
            extractTarFile(tarFile, outDir);

            // 3. 解析解压出来的文件
            File[] files = outDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    parseFile(f);
                }
            }

            // 4. 删除解压目录和 tar 文件
            deleteFileOrDir(outDir);
            deleteFileOrDir(tarFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
