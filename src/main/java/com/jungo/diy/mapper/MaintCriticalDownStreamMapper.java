package com.jungo.diy.mapper;

import com.jungo.diy.entity.ApiDailyPerformanceEntity;
import com.jungo.diy.entity.MaintCriticalDownStreamEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * @author lichuang3
 * @date 2025-02-17 19:18
 */
@Mapper
public interface MaintCriticalDownStreamMapper {

    // 批量插入数据
    int batchInsert(List<MaintCriticalDownStreamEntity> list);


}
