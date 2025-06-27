package com.jungo.diy.mapper;

import com.jungo.diy.entity.ApiDailyPerformanceEntity;
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
public interface ApiDailyPerformanceMapper {

    // 查询所有用户
    @Select("SELECT * FROM api_daily_performance")
    List<ApiDailyPerformanceEntity> findAll();

    int insert(ApiDailyPerformanceEntity apiDailyPerformance);

    int batchInsert(List<ApiDailyPerformanceEntity> list);

    List<ApiDailyPerformanceEntity> findUrl99Line(@Param("host")String host, @Param("url")String url, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<ApiDailyPerformanceEntity> findAllByDate(LocalDate startDate);

    List<ApiDailyPerformanceEntity> getSlowRequestRate(@Param("url")String url, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<ApiDailyPerformanceEntity> getRecordsByPkidRange(@Param("startId") long startId, @Param("endId") long endId);
}
