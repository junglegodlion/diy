package com.jungo.diy.entity;// MaintCriticalDownstreamList.java
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 维保关键下游清单实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintCriticalDownStreamEntity {
    /**
     * 自增主键
     */
    private Integer id;
    
    /**
     * 客户端
     */
    private String clientAppId;
    
    /**
     * 服务端
     */
    private String serverAppId;
    
    /**
     * 接口
     */
    private String interfaceUrl;
    
    /**
     * 99线(ms)
     */
    private Integer p99;
    
    /**
     * 日期
     */
    private Date date;
    
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
