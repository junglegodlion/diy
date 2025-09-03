package com.jungo.diy.test;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.var;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Stream;

public class RemoteTarProcessor {

    // 下载远程 tar 文件到本地
    public static File downloadTar(String url, String savePath) throws IOException {
        System.out.println("开始下载: " + url);
        HttpResponse<byte[]> response = Unirest.get(url)
                .header("Accept", "*/*")
                .asBytes();

        if (response.getStatus() != 200) {
            throw new IOException("下载失败，HTTP状态码：" + response.getStatus());
        }

        File tarFile = new File(savePath);
        try (FileOutputStream fos = new FileOutputStream(tarFile)) {
            fos.write(response.getBody());
        }
        System.out.println("下载完成: " + tarFile.getAbsolutePath());
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
