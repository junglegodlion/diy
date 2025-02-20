package com.jungo.diy.mapper;

import com.jungo.diy.entity.GateWayDailyPerformanceEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author lichuang3
 * @date 2025-02-17 19:18
 */
@Mapper
public interface GateWayDailyPerformanceMapper {

    int batchInsert(List<GateWayDailyPerformanceEntity> gateWayDailyPerformanceEntities);
}
