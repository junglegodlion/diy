package com.jungo.diy.controller;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.util.UUID;

public class Main {
    public static void main(String[] args) {
        String url = "https://xapi.quyundong.com/Api/Venues/bookTable";

        // 动态生成时间戳和 noncestr
        long clientTime = System.currentTimeMillis();
        String noncestr = clientTime + UUID.randomUUID().toString().substring(0, 4);

        // ⚠️ 注意：api_sign 通常基于其他参数签名，如果你有签名逻辑可在此处生成
        // 当前为了演示仍使用你提供的固定值（可能会被判为重复请求）
        String apiSign = "c111d878d9b2db2ffcaa9dc7093f1cd41c39607db7c4261e19f09a48ba5e2e62";

        HttpResponse<String> response = Unirest.get(url)
                .queryString("utm_source", "miniprogram")
                .queryString("utm_medium", "wechatapp")
                .queryString("client_time", String.valueOf(clientTime))
                .queryString("sign_key", "1751853395064924700")
                .queryString("device_id", "b3d966ea5010164124e81b05b9aa3e60")
                .queryString("app_version", "3.3.1")
                .queryString("latitude", "31.16660183")
                .queryString("longitude", "121.38725857")
                .queryString("noncestr", noncestr)
                .queryString("access_token", "k0u4vY6ElNGGfQ8eQJ5ONFFXVHQZlfhi")
                .queryString("login_token", "c344e0f829adf4e9b654caaa35ab34f5")
                .queryString("city_id", "321")
                .queryString("city_name", "上海")
                .queryString("area_id", "0")
                .queryString("ver", "2.9")
                .queryString("venues_id", "26106")
                .queryString("cat_id", "1")
                .queryString("book_date", "2025-07-13")
                .queryString("raise_package_id", "0")
                .queryString("display_height", "2079.00")
                .queryString("display_width", "960.00")
                .queryString("phone_encode", "ebJfin4648Zt2tGvPI6mhA==")
                .queryString("api_sign", apiSign) // ✅ 有能力建议动态生成
                .asString();

        System.out.println("Status: " + response.getStatus());
        System.out.println("Body:\n" + response.getBody());
    }
}
