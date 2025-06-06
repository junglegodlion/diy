package com.jungo.diy.test;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ToStringParser2 {

    public static void main(String[] args) throws Exception {
        // 从文件读取测试数据
        Path path = Paths.get("src/main/resources/testdata/object");
        if(!Files.exists(path)) {
            throw new IllegalStateException("Test data file not found: " + path);
        }
        String input = new String(Files.readAllBytes(path));
        Map<String, Object> jsonMap = parseToMap(input);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonMap);
        System.out.println(json);
    }

    public static Map<String, Object> parseToMap(String input) {
        int firstParen = input.indexOf('(');
        String rootName = input.substring(0, firstParen);
        String body = input.substring(firstParen + 1, input.lastIndexOf(')'));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(rootName, parseBody(body));
        return map;
    }

    private static Object parseBody(String body) {
        Map<String, Object> map = new LinkedHashMap<>();
        List<String> tokens = tokenize(body);

        for (String token : tokens) {
            int eqIdx = token.indexOf('=');
            if (eqIdx == -1) continue;

            String key = token.substring(0, eqIdx).trim();
            String value = token.substring(eqIdx + 1).trim();

            if (isObjectLike(value)) {
                map.put(key, parseToMap(value));
            } else if (value.startsWith("[") && value.endsWith("]")) {
                map.put(key, parseList(value));
            } else {
                map.put(key, value);
            }
        }

        return map;
    }

    private static List<Object> parseList(String listStr) {
        List<Object> list = new ArrayList<>();
        String content = listStr.substring(1, listStr.length() - 1);
        List<String> tokens = tokenize(content);

        for (String item : tokens) {
            item = item.trim();
            if (isObjectLike(item)) {
                list.add(parseToMap(item));
            } else {
                list.add(item);
            }
        }

        return list;
    }

    private static List<String> tokenize(String str) {
        List<String> tokens = new ArrayList<>();
        int depth = 0;
        StringBuilder sb = new StringBuilder();
        char[] chars = str.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == ',' && depth == 0) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                if (c == '(' || c == '[') depth++;
                else if (c == ')' || c == ']') depth--;
                sb.append(c);
            }
        }

        if (sb.length() > 0) {
            tokens.add(sb.toString().trim());
        }

        return tokens;
    }

    private static boolean isObjectLike(String value) {
        return value.matches("^[a-zA-Z_][a-zA-Z0-9_]*\\(.*\\)$");
    }
}
