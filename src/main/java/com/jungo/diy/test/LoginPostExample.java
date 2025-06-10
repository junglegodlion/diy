package com.jungo.diy.test;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.util.HashMap;
import java.util.Map;

public class LoginPostExample {
    public static void main(String[] args) {

        // 查询参数
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("username", "lichuang3");
        queryParams.put("password", "TrHNjxc8YTQUf90Izf3bBQ==");

        HttpResponse<String> loginResponse = Unirest.post("https://int-service-elk.tuhuyun.cn/login")
                .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("accept-language", "zh-CN,zh;q=0.9")
                .header("cache-control", "max-age=0")
                .header("content-type", "application/x-www-form-urlencoded")
                .header("priority", "u=0, i")
                .header("sec-ch-ua", "\"Google Chrome\";v=\"137\", \"Chromium\";v=\"137\", \"Not/A)Brand\";v=\"24\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"Windows\"")
                .header("sec-fetch-dest", "document")
                .header("sec-fetch-mode", "navigate")
                .header("sec-fetch-site", "same-origin")
                .header("sec-fetch-user", "?1")
                .header("upgrade-insecure-requests", "1")
                .queryString(queryParams)
                .asString();
        // 获取响应中的Cookie
        String cookies = loginResponse.getHeaders().getFirst("Set-Cookie");

        System.out.println("Status: " + loginResponse.getStatus());
        System.out.println("Response body: ");
        System.out.println(loginResponse.getBody());
        System.out.println("cookies: " + cookies);
    }
}
