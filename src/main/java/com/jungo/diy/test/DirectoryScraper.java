package com.jungo.diy.test;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class DirectoryScraper {

    public static void main(String[] args) throws IOException {
        String directoryUrl = "https://sre.tuhuyun.cn/upload/log_data_export/";

        Document doc = Jsoup.connect(directoryUrl).get();
        Elements links = doc.select("a[href]");

        for (Element link : links) {
            String href = link.attr("href");

            // 跳过父目录 ".."
            if (href.equals("../")) {
                continue;
            }

            String fullUrl = directoryUrl + href;
            System.out.println(fullUrl);
        }
    }
}
