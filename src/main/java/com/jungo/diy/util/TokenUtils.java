package com.jungo.diy.util;

/**
 * @author lichuang3
 * @date 2025-03-21 12:27
 */
public class TokenUtils {

    public static final String COMMA = ":";

    private TokenUtils() {
    }

    // 生成token
    // 生成token
    public static String generateToken(String host, String url) {
        if (host == null || url == null) {
            throw new IllegalArgumentException("host 和 url 不能为 null");
        }
        return host + COMMA + url;
    }

    // 分解token
    public static String[] parseToken(String token) {
        if (token == null) {
            throw new IllegalArgumentException("token 不能为 null");
        }
        String[] parts = token.split(COMMA);
        if (parts.length != 2) {
            throw new IllegalArgumentException("token 格式不正确");
        }
        return parts;
    }

}
