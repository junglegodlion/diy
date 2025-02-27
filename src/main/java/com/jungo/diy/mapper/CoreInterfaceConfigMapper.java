package com.jungo.diy.mapper;

import com.jungo.diy.entity.CoreInterfaceConfigEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author lichuang3
 * @date 2025-02-25 19:12
 */
@Mapper
public interface CoreInterfaceConfigMapper {
    // 批量插入CoreInterfaceConfigEntity
    int batchInsert(List<CoreInterfaceConfigEntity> list);
    // 获取指定接口类型的接口，按接口排序值排序
    List<CoreInterfaceConfigEntity> getCoreInterfaceConfigByInterfaceType(Integer interfaceType);
}
