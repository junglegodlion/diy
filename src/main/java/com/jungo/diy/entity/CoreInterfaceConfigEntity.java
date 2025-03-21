package com.jungo.diy.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author lichuang3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoreInterfaceConfigEntity {
    /**
     * 自增主键
     */
    private Integer id;

    /**
     * 页面名称
     */
    private String pageName;

    /**
     * 接口路径
     */
    private String interfaceUrl;

    /**
     * host
     */
    private String host;

    /**
     * 99线基线目标(ms)
     */
    private Integer p99Target;

    /**
     * 慢请求率基线目标(%)
     */
    private BigDecimal slowRequestRateTarget;

    /**
     * 接口类型(1-默认类型 关键路径)
     */
    private Integer interfaceType;

    /**
     * 接口排序值
     */
    private Integer sortOrder;

    /**
     * 接口负责人
     */
    private String owner;

    /**
     * 逻辑删除标记(0-正常 1-删除)
     */
    private Integer isDeleted;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}