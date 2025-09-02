package com.jungo.diy.test;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TarReaderExample {
    private static final int BUFFER_SIZE = 8192;
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;
    private static final int MAX_REDIRECTS = 5;

    public static void main(String[] args) {
        String originalUrl = "http://sre.tuhuyun.cn/upload/log_data_export/2025-09-02/lichuang-wanaccess.tar";

        System.out.println("开始处理URL: " + originalUrl);

        try {
            // 处理重定向
            String finalUrl = resolveRedirects(originalUrl);
            System.out.println("重定向到: " + finalUrl);

            if (!finalUrl.equals(originalUrl)) {
                System.out.println("注意: URL已被重定向");
            }

            // 连接到最终URL
            URL url = new URL(finalUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("User-Agent", "Java-TAR-Reader/1.0");
            connection.setInstanceFollowRedirects(false); // 手动处理重定向

            System.out.println("建立连接到最终URL...");
            int responseCode = connection.getResponseCode();
            System.out.println("最终HTTP响应码: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("最终HTTP错误: " + responseCode + " - " + connection.getResponseMessage());
                return;
            }

            long contentLength = connection.getContentLengthLong();
            System.out.println("内容长度: " + contentLength + " bytes");

            if (contentLength == 0) {
                System.err.println("文件为空");
                return;
            }

            // 读取文件内容
            try (InputStream rawStream = connection.getInputStream();
                 BufferedInputStream buffered = new BufferedInputStream(rawStream, BUFFER_SIZE)) {

                // 检测文件格式并创建相应的输入流
                InputStream decompressedStream = detectCompressionFormat(buffered);

                // 解析TAR内容
                parseTarContent(decompressedStream);
            }

        } catch (Exception e) {
            System.err.println("处理过程中发生错误: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 解析重定向链，返回最终URL
     */
    private static String resolveRedirects(String urlStr) throws Exception {
        String currentUrl = urlStr;
        int redirectCount = 0;

        while (redirectCount < MAX_REDIRECTS) {
            URL url = new URL(currentUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setInstanceFollowRedirects(false); // 禁用自动重定向
            connection.setRequestProperty("User-Agent", "Java-TAR-Reader/1.0");

            int responseCode = connection.getResponseCode();

            // 如果是重定向，获取新的Location
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER) {

                String newUrl = connection.getHeaderField("Location");
                if (newUrl == null) {
                    throw new Exception("重定向响应中没有Location头");
                }

                // 处理相对URL
                if (!newUrl.startsWith("http")) {
                    URL baseUrl = new URL(currentUrl);
                    newUrl = new URL(baseUrl, newUrl).toString();
                }

                System.out.println("重定向 " + (redirectCount + 1) + ": " + currentUrl + " -> " + newUrl);
                currentUrl = newUrl;
                redirectCount++;
                connection.disconnect();
            } else {
                connection.disconnect();
                return currentUrl;
            }
        }

        throw new Exception("重定向次数超过限制 (" + MAX_REDIRECTS + ")");
    }

    /**
     * 检测压缩格式并创建相应的输入流
     */
    private static InputStream detectCompressionFormat(BufferedInputStream buffered) throws Exception {
        buffered.mark(10);
        byte[] header = new byte[4];
        int headerBytes = buffered.read(header);
        buffered.reset();

        System.out.println("文件头: " + bytesToHex(header));

        // GZIP魔数: 1F 8B
        if (header.length >= 2 && header[0] == 0x1F && header[1] == (byte)0x8B) {
            System.out.println("检测到GZIP压缩格式");
            return new GzipCompressorInputStream(buffered);
        }
        // BZIP2魔数: 42 5A 68
        else if (header.length >= 3 && header[0] == 0x42 && header[1] == 0x5A && header[2] == 0x68) {
            System.out.println("检测到BZIP2压缩格式");
            return new BZip2CompressorInputStream(buffered);
        }
        else {
            System.out.println("未检测到压缩格式，按普通TAR处理");
            return buffered;
        }
    }

    /**
     * 解析TAR文件内容
     */
    private static void parseTarContent(InputStream inputStream) throws Exception {
        try (TarArchiveInputStream tarInput = new TarArchiveInputStream(inputStream)) {
            ArchiveEntry entry;
            int fileCount = 0;
            boolean hasEntries = false;

            System.out.println("开始解析TAR内容...");

            while ((entry = tarInput.getNextEntry()) != null) {
                hasEntries = true;
                fileCount++;

                System.out.printf("条目 %d: %s (大小: %d bytes, 目录: %b)%n",
                        fileCount, entry.getName(), entry.getSize(), entry.isDirectory());

                if (!entry.isDirectory() && entry.getSize() > 0) {
                    ByteArrayOutputStream contentStream = new ByteArrayOutputStream();
                    long copied = IOUtils.copy(tarInput, contentStream);
                    System.out.printf("  实际读取: %d bytes%n", copied);

                    if (copied > 0) {
                        String content = contentStream.toString(StandardCharsets.UTF_8.name());
                        System.out.printf("  内容预览: %s...%n",
                                content.substring(0, Math.min(100, content.length())));
                    }
                }
                System.out.println("  ---");
            }

            if (!hasEntries) {
                System.out.println("警告: 未找到任何TAR条目");
            } else {
                System.out.println("解析完成，共找到 " + fileCount + " 个条目");
            }
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
