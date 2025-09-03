package com.jungo.diy.test;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class RemoteTarParser {

    public static void main(String[] args) {
        String url = "https://sre.tuhuyun.cn/upload/log_data_export/2025-09-03/lichuang-wanaccess.tar";
        try {
            // 1. 使用 Unirest 下载远程 tar 文件为 byte[]
            HttpResponse<byte[]> response = Unirest.get(url)
                    .header("Accept", "*/*")
                    .asBytes();

            if (response.getStatus() != 200) {
                System.err.println("下载失败，HTTP状态码：" + response.getStatus());
                return;
            }

            // 2. 包装成 InputStream，传给 TarArchiveInputStream
            try (InputStream inputStream = new ByteArrayInputStream(response.getBody());
                 TarArchiveInputStream tais = new TarArchiveInputStream(inputStream)) {

                TarArchiveEntry entry;
                while ((entry = tais.getNextTarEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }

                    System.out.println("=== 文件: " + entry.getName() + " ===");

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    IOUtils.copy(tais, baos);
                    String content = baos.toString(String.valueOf(StandardCharsets.UTF_8));

                    for (String line : content.split("\\r?\\n")) {
                        if (!line.trim().isEmpty()) {
                            System.out.println(line);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
