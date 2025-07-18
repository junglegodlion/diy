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
    // 正常请求率
    private float normalRequestRate;
    // status非200错误率
    private Float not200errorRate;

    // 成功率
    // status非10000错误率
    private Float successRate;

    public float getSuccessRate() {
        // 使用明确的变量名表示计算逻辑
        float totalErrorRate = not200errorRate + errorRate;
        // 使用Math.max确保结果不低于0，防止负数出现
        return Math.max(0f, 1f - totalErrorRate);
    }

    public float getErrorRate() {
        if (totalRequests == 0) {
            return 0.0f;
        }
        return (float) errorRequests / totalRequests;
    }

    public float getNormalRequestRate() {
        return 1 - getErrorRate();
    }
}
