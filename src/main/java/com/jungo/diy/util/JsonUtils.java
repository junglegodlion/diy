package com.jungo.diy.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;


/**
 * @author lichuang3
 */
@Slf4j
public class JsonUtils {

    public static String objectToJson(Object object) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.error("JsonUtils#objectToJson,出现异常！", e);
        }
        return null;
    }

}