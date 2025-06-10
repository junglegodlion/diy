package com.jungo.diy.model;

import lombok.Data;

/**
 * @author lichuang3
 * @date 2025-06-10 17:09
 */
@Data
public class BusinessStatusErrorModel {

    // appId
    private String appId;
    // 接口
    private String url;
    // 总请求数
    private int totalRequests;
    // 业务异常请求数
    private int errorRequests;
    // 业务异常率
    private float errorRate;

    public float getErrorRate() {
        if (totalRequests == 0) {
            return 0.0f;
        }
        return (float) errorRequests / totalRequests;
    }
}
