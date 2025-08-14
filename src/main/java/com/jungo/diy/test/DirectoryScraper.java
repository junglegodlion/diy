package com.jungo.diy.test;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DirectoryScraper {

    public static void main(String[] args) throws IOException {
        String directoryUrl = "https://sre.tuhuyun.cn/upload/log_data_export/2025-08-14/";
        String localDownloadPath = "./downloads/";

        Document doc = Jsoup.connect(directoryUrl).get();
        Elements links = doc.select("a[href]");

        List<String> urls = new ArrayList<>();
        for (Element link : links) {
            String href = link.attr("href");

            // 跳过父目录 ".."
            if (href.equals("../")) {
                continue;
            }

            String fullUrl = directoryUrl + href;
            try {
                String decodedUrl = URLDecoder.decode(fullUrl, "UTF-8");
                urls.add(decodedUrl);
                downloadFile(decodedUrl, localDownloadPath);
            } catch (UnsupportedEncodingException e) {
                downloadFile(fullUrl, localDownloadPath);
            }
        }
    }

    private static void downloadFile(String fileURL, String localDownloadPath) throws IOException {
        try {
            String fileName = extractFileNameFromUrl(fileURL);
            File saveFile = new File(localDownloadPath, fileName);

            HttpResponse<byte[]> response = Unirest.get(fileURL).asBytes();

            if (response.getStatus() == 200) {
                // 确保目录存在
                saveFile.getParentFile().mkdirs();

                try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                    fos.write(response.getBody());
                }
                System.out.println("下载完成: " + saveFile.getAbsolutePath());
            } else {
                System.err.println("下载失败，HTTP状态码: " + response.getStatus() + ", URL: " + fileURL);
            }
        } catch (Exception e) {
            System.err.println("下载异常: " + e.getMessage() + ", URL: " + fileURL);
        }
    }
    private static String extractFileNameFromUrl(String url) {
        try {
            String decodedUrl = URLDecoder.decode(url, "UTF-8");
            return decodedUrl.substring(decodedUrl.lastIndexOf('/') + 1);
        } catch (Exception e) {
            // 如果解析失败，使用默认文件名
            return "downloaded_file_" + System.currentTimeMillis() + ".dat";
        }
    }

}
