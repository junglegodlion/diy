package com.jungo.diy.mapper;

import com.jungo.diy.entity.ApiDailyPerformanceEntity;
import com.jungo.diy.entity.GateWayDailyPerformanceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * @author lichuang3
 * @date 2025-02-17 19:18
 */
@Mapper
public interface GateWayDailyPerformanceMapper {

    int batchInsert(List<GateWayDailyPerformanceEntity> gateWayDailyPerformanceEntities);

    List<GateWayDailyPerformanceEntity> getPerformanceByYear(@Param("host") String host, @Param("startDate") LocalDate startDate);

    List<GateWayDailyPerformanceEntity> getPerformanceByDate(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDat);
}
