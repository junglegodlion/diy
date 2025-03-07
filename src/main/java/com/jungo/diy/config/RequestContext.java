package com.jungo.diy.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lichuang3
 */
public class RequestContext {
    private static final ThreadLocal<Map<String, Object>> CONTEXT = new ThreadLocal<>();

    // 新增动态参数操作方法
    public static void put(String key, Object value) {
        Map<String, Object> current = CONTEXT.get()  != null ?
                // 创建可写副本
                new HashMap<>(CONTEXT.get())  :
                new HashMap<>();

        current.put(key,  value);
        // 保持不可变
        CONTEXT.set(Collections.unmodifiableMap(current));
    }

    // 批量写入优化
    public static void putAll(Map<String, Object> additions) {
        Map<String, Object> current = new HashMap<>(getExt());
        current.putAll(additions);
        CONTEXT.set(Collections.unmodifiableMap(current));
    }

    public static void setExt(Map<String, Object> ext) {
        CONTEXT.set(Collections.unmodifiableMap(ext));
    }

    public static Map<String, Object> getExt() {
        return CONTEXT.get()  != null ?
                CONTEXT.get()  :
               Collections.emptyMap();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static void debugPrint() {
        getExt().forEach((k, v) ->
                System.out.printf("[CONTEXT]  %s=%s%n", k, v));
    }


    public static <T> T getAs(String key, Class<T> type) {
        Object value = getExt().get(key);
        return type.isInstance(value)  ? type.cast(value)  : null;
    }
}
