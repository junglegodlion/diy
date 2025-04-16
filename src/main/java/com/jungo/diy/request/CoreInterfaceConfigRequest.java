package com.jungo.diy.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author lichuang3
 * @date 2025-04-16 19:41
 */
@Data
@ApiModel(description = "核心接口配置请求实体")
public class CoreInterfaceConfigRequest {

    /**
     * 页面名称
     */
    @ApiModelProperty(value = "页面名称", example = "首页")
    private String pageName;

    /**
     * 接口路径
     */
    @ApiModelProperty(value = "接口路径", example = "/api/v1/getData")
    private String interfaceUrl;

    /**
     * host
     */
    @ApiModelProperty(value = "host", example = "https://example.com")
    private String host;

    /**
     * 99线基线目标(ms)
     */
    @ApiModelProperty(value = "99线基线目标(ms)", example = "200")
    private Integer p99Target;

    /**
     * 慢请求率基线目标(%)
     */
    @ApiModelProperty(value = "慢请求率基线目标(%)", example = "0.5")
    private BigDecimal slowRequestRateTarget;

    /**
     * 接口类型(1-默认类型 关键路径)
     */
    @ApiModelProperty(value = "接口类型(1-默认类型 关键路径)", example = "1")
    private Integer interfaceType;

    /**
     * 接口排序值
     */
    @ApiModelProperty(value = "接口排序值", example = "10")
    private Integer sortOrder;

    /**
     * 接口负责人
     */
    @ApiModelProperty(value = "接口负责人", example = "张三")
    private String owner;
}
