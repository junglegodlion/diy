package com.jungo.diy.repository;

import com.jungo.diy.entity.ApiDailyPerformanceEntity;
import com.jungo.diy.entity.GateWayDailyPerformanceEntity;
import com.jungo.diy.mapper.ApiDailyPerformanceMapper;
import com.jungo.diy.mapper.GateWayDailyPerformanceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author lichuang3
 * @date 2025-02-21 16:21
 */
@Repository
public class PerformanceRepository {

    @Autowired
    ApiDailyPerformanceMapper apiDailyPerformanceMapper;

    @Autowired
    GateWayDailyPerformanceMapper gateWayDailyPerformanceMapper;

    @Transactional(rollbackFor = Exception.class)
    public void writePerformanceData2DB(List<GateWayDailyPerformanceEntity> gateWayDailyPerformanceEntities, List<ApiDailyPerformanceEntity> apiDailyPerformanceEntities) {
        // 判断gateWayDailyPerformanceEntities不为空且不为null
        if (gateWayDailyPerformanceEntities != null && !gateWayDailyPerformanceEntities.isEmpty()) {
            gateWayDailyPerformanceMapper.batchInsert(gateWayDailyPerformanceEntities);
        }
        if (apiDailyPerformanceEntities != null && !apiDailyPerformanceEntities.isEmpty()) {
            apiDailyPerformanceMapper.batchInsert(apiDailyPerformanceEntities);
        }
    }
}
