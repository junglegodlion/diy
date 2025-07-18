package com.jungo.diy.model;

import lombok.Data;

/**
 * @author lichuang3
 * @date 2025-04-25 15:44
 */
@Data
public class UrlStatusErrorModel {
    // token
    private String token;
    // 主机
    private String host;
    // 接口
    private String url;
    // 状态码
    private Integer status;
    // 次数
    private Integer count;
    // 请求总数
    private Integer totalCount;
    // 占比
    private Float percentRate;
    // 非200的请求总数
    private Integer not200Count;
    // status非200错误率
    private Float not200errorRate;

    // status非10000错误率
    private Float not10000errorRate;

    // 成功率
    // status非10000错误率
    private Float successRate;

    public float getSuccessRate() {
        // 使用明确的变量名表示计算逻辑
        float totalErrorRate = not200errorRate + not10000errorRate;
        // 使用Math.max确保结果不低于0，防止负数出现
        return Math.max(0f, 1f - totalErrorRate);
    }



    public float getNot200errorRate() {
        if (not200Count == null || totalCount == null) {
            // 或者抛出异常，根据业务逻辑决定
            return 0.0f;
        }
        return (float) not200Count / totalCount;
    }

    public float getPercentRate() {
        if (count == null || totalCount == null) {
            // 或者抛出异常，根据业务逻辑决定
            return 0.0f;
        }
        return (float) count / totalCount;
    }

    public String getToken() {
        return host + ":" + url;
    }
}
