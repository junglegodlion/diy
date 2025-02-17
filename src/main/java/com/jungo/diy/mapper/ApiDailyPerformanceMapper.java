package com.jungo.diy.mapper;

import com.jungo.diy.entity.ApiDailyPerformanceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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
}
