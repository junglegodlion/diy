package com.jungo.diy.service;

import com.jungo.diy.entity.ApiDailyPerformanceEntity;
import com.jungo.diy.mapper.ApiDailyPerformanceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author lichuang3
 * @date 2025-02-17 19:20
 */
@Service
public class ApiDailyPerformanceService {

    @Autowired
    private ApiDailyPerformanceMapper apiDailyPerformanceMapper;

    public List<ApiDailyPerformanceEntity> getAllApiDailyPerformanceEntities() {
        return apiDailyPerformanceMapper.findAll();
    }
}
