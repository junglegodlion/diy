package com.jungo.diy.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jungo.diy.config.RequestContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ExtInterceptor implements HandlerInterceptor {
   
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        // 从请求中获取ext参数（支持URL参数和JSON body）
        Map<String, Object> extParams = new HashMap<>();
       
        // 1. 获取URL参数
        request.getParameterMap().forEach((k,  v) -> {
            if(k.startsWith("ext."))  {
                extParams.put(k.substring(4),  v.length  > 0 ? v[0] : null);
            }
        });

        // 2. 处理JSON Body（需配合@RequestBody使用）
        // if(request.getContentType()  != null &&
        //    request.getContentType().contains("application/json"))  {
        //     try {
        //         BufferedReader reader = request.getReader();
        //         String jsonBody = reader.lines().collect(Collectors.joining());
        //         JsonNode rootNode = new ObjectMapper().readTree(jsonBody);
        //
        //         if(rootNode.has("ext"))  {
        //             rootNode.get("ext").fields().forEachRemaining(entry  -> {
        //                 extParams.put(entry.getKey(),  entry.getValue().asText());
        //             });
        //         }
        //     } catch (IOException e) {
        //         // 异常处理逻辑
        //     }
        // }

        // 设置到线程上下文
        if(!extParams.isEmpty())  {
            RequestContext.setExt(extParams);
        }
       
        return true;
    }
}
