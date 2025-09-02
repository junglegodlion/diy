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
    private static final int PREVIEW_LENGTH = 100;

    public static void main(String[] args) {
        String originalUrl = "http://sre.tuhuyun.cn/upload/log_data_export/2025-09-02/lichuang-wanaccess.tar";
        System.out.println("开始处理URL: " + originalUrl);

        try {
            String finalUrl = resolveRedirects(originalUrl);
            if (!finalUrl.equals(originalUrl)) {
                System.out.println("重定向到: " + finalUrl);
            }

            HttpURLConnection connection = createConnection(finalUrl);
            validateResponse(connection);

            try (InputStream rawStream = connection.getInputStream();
                 BufferedInputStream buffered = new BufferedInputStream(rawStream, BUFFER_SIZE);
                 InputStream decompressedStream = detectCompressionFormat(buffered)) {

                parseTarContent(decompressedStream);
            }

        } catch (Exception e) {
            System.err.println("处理错误: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String resolveRedirects(String urlStr) throws Exception {
        String currentUrl = urlStr;
        int redirectCount = 0;

        while (redirectCount < MAX_REDIRECTS) {
            HttpURLConnection connection = createConnection(currentUrl);
            int responseCode = connection.getResponseCode();

            if (isRedirect(responseCode)) {
                String newUrl = getRedirectUrl(connection, currentUrl);
                System.out.println("重定向 " + (++redirectCount) + ": " + currentUrl + " -> " + newUrl);
                currentUrl = newUrl;
            } else {
                return currentUrl;
            }
        }
        throw new Exception("重定向次数超过限制 (" + MAX_REDIRECTS + ")");
    }

    private static HttpURLConnection createConnection(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestProperty("User-Agent", "Java-TAR-Reader/1.0");
        connection.setInstanceFollowRedirects(false);
        return connection;
    }

    private static boolean isRedirect(int responseCode) {
        return responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_SEE_OTHER;
    }

    private static String getRedirectUrl(HttpURLConnection connection, String currentUrl) throws Exception {
        String newUrl = connection.getHeaderField("Location");
        if (newUrl == null) throw new Exception("重定向响应中没有Location头");

        if (!newUrl.startsWith("http")) {
            URL baseUrl = new URL(currentUrl);
            newUrl = new URL(baseUrl, newUrl).toString();
        }
        connection.disconnect();
        return newUrl;
    }

    private static void validateResponse(HttpURLConnection connection) throws Exception {
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP错误: " + responseCode + " - " + connection.getResponseMessage());
        }

        long contentLength = connection.getContentLengthLong();
        if (contentLength == 0) {
            throw new Exception("文件为空");
        }
        System.out.println("内容长度: " + contentLength + " bytes");
    }

    private static InputStream detectCompressionFormat(BufferedInputStream buffered) throws Exception {
        buffered.mark(10);
        byte[] header = new byte[4];
        buffered.read(header);
        buffered.reset();

        System.out.println("文件头: " + bytesToHex(header));

        if (isGzip(header)) {
            System.out.println("检测到GZIP压缩格式");
            return new GzipCompressorInputStream(buffered);
        } else if (isBzip2(header)) {
            System.out.println("检测到BZIP2压缩格式");
            return new BZip2CompressorInputStream(buffered);
        } else {
            System.out.println("未检测到压缩格式");
            return buffered;
        }
    }

    private static boolean isGzip(byte[] header) {
        return header.length >= 2 && header[0] == 0x1F && header[1] == (byte)0x8B;
    }

    private static boolean isBzip2(byte[] header) {
        return header.length >= 3 && header[0] == 0x42 && header[1] == 0x5A && header[2] == 0x68;
    }

    private static void parseTarContent(InputStream inputStream) throws Exception {
        try (TarArchiveInputStream tarInput = new TarArchiveInputStream(inputStream)) {
            ArchiveEntry entry;
            int fileCount = 0;

            while ((entry = tarInput.getNextEntry()) != null) {
                fileCount++;
                System.out.printf("条目 %d: %s (大小: %d bytes, 目录: %b)%n",
                        fileCount, entry.getName(), entry.getSize(), entry.isDirectory());

                if (!entry.isDirectory() && entry.getSize() > 0) {
                    ByteArrayOutputStream contentStream = new ByteArrayOutputStream();
                    IOUtils.copy(tarInput, contentStream);
                    String content = contentStream.toString(StandardCharsets.UTF_8.name());
                    System.out.printf("  内容预览: %s...%n", content.substring(0, Math.min(PREVIEW_LENGTH, content.length())));
                }
                System.out.println("  ---");
            }

            System.out.println(fileCount == 0 ? "警告: 未找到任何TAR条目" : "解析完成，共找到 " + fileCount + " 个条目");
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }
}
