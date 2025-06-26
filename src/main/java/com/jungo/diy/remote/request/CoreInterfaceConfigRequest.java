package com.jungo.diy.remote.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author lichuang3
 * @date 2025-06-26 14:17
 */
@Data
public class CoreInterfaceConfigRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "页面名称不能为空")
    @Size(max = 50, message = "页面名称长度不能超过50")
    @ApiModelProperty(value = "页面名称，用于归类该接口属于哪个前端页面", required = true)
    private String pageName;

    @NotBlank(message = "接口路径不能为空")
    @Size(max = 255, message = "接口路径长度不能超过255")
    @Pattern(regexp = "^/.*", message = "接口路径必须以/开头")
    @ApiModelProperty(value = "接口路径，记录具体的API请求地址", required = true)
    private String interfaceUrl;

    @ApiModelProperty(value = "P99性能基线目标，单位毫秒(ms)，表示99百分位响应时间要求", example = "200")
    private Integer p99Target;

    @ApiModelProperty(value = "慢请求率基线目标，单位百分比(%)，表示超出阈值的请求占比", example = "0.5")
    private BigDecimal slowRequestRateTarget;

    @NotNull(message = "接口类型不能为空")
    @Min(value = 1, message = "接口类型最小值为1")
    @ApiModelProperty(value = "接口类型：1表示默认类型(关键路径)，可根据需要扩展其他类型", example = "1")
    private Integer interfaceType;

    @NotNull(message = "排序值不能为空")
    @Min(value = 1, message = "排序值最小为1")
    @ApiModelProperty(value = "接口排序值，用于前端展示或查询排序，值越小排序越靠前", example = "10")
    private Integer sortOrder;

    @ApiModelProperty(value = "接口负责人，记录接口归属责任人")
    private String owner;

    @Size(max = 100, message = "主机域名长度不能超过100")
    @ApiModelProperty(value = "所属主机或网关域名，默认值为cl-gateway.tuhu.cn", example = "cl-gateway.tuhu.cn")
    private String host;
}
