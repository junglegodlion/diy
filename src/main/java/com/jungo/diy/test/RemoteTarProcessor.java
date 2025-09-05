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
        // 输入参数验证
        if (tarFile == null || !tarFile.exists() || !tarFile.isFile()) {
            throw new IllegalArgumentException("TAR文件不存在或不是有效文件");
        }
        if (outputDir == null) {
            throw new IllegalArgumentException("输出目录不能为空");
        }

        // 创建输出目录（如果不存在）
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("无法创建输出目录: " + outputDir.getAbsolutePath());
        }

        try (TarArchiveInputStream tais = new TarArchiveInputStream(
                new BufferedInputStream(new FileInputStream(tarFile)))) {

            TarArchiveEntry entry;
            // 使用推荐的getNextEntry()替代已弃用的getNextTarEntry()
            while ((entry = (TarArchiveEntry) tais.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    // 处理目录条目
                    File dir = new File(outputDir, entry.getName());
                    if (!dir.exists() && !dir.mkdirs()) {
                        log.warn("无法创建目录: {}", dir.getAbsolutePath());
                    }
                    continue;
                }

                File outFile = new File(outputDir, entry.getName());

                // 安全检查：防止路径遍历攻击
                if (!outFile.getCanonicalPath().startsWith(outputDir.getCanonicalPath())) {
                    throw new IOException("无效的文件路径，可能存在安全风险: " + entry.getName());
                }

                // 确保父目录存在
                File parentDir = outFile.getParentFile();
                if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                    throw new IOException("无法创建父目录: " + parentDir.getAbsolutePath());
                }

                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    IOUtils.copy(tais, fos);
                }

                log.debug("解压文件: {}", outFile.getName());
            }
        }
        log.info("TAR文件解压完成: {}", tarFile.getName());
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
        // 输入验证
        if (file == null) {
            throw new IllegalArgumentException("文件对象不能为空");
        }

        if (!file.exists()) {
            log.debug("文件不存在，无需删除: {}", file.getAbsolutePath());
            return;
        }

        try {
            if (file.isDirectory()) {
                // 使用Files.walk()直接删除目录及其内容
                try (Stream<Path> paths = Files.walk(file.toPath())) {
                    paths.sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                    log.debug("已删除: {}", path);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(
                                            String.format("删除文件失败: %s, 原因: %s",
                                                    path, e.getMessage()), e);
                                }
                            });
                }
            } else {
                // 删除单个文件
                Files.deleteIfExists(file.toPath());
                log.debug("已删除文件: {}", file.getAbsolutePath());
            }

            log.info("成功删除: {}", file.getAbsolutePath());

        } catch (UncheckedIOException e) {
            // 重新抛出原始IOException，保留详细的错误信息
            throw new IOException(String.format("删除操作失败: %s", file.getAbsolutePath()),
                    e.getCause());
        }
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
