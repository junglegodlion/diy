package com.jungo.diy.repository;

import com.jungo.diy.entity.ApiDailyPerformanceEntity;
import com.jungo.diy.entity.CoreInterfaceConfigEntity;
import com.jungo.diy.entity.GateWayDailyPerformanceEntity;
import com.jungo.diy.enums.InterfaceTypeEnum;
import com.jungo.diy.mapper.ApiDailyPerformanceMapper;
import com.jungo.diy.mapper.CoreInterfaceConfigMapper;
import com.jungo.diy.mapper.GateWayDailyPerformanceMapper;
import com.jungo.diy.model.InterfacePerformanceModel;
import com.jungo.diy.model.SlowRequestRateModel;
import com.jungo.diy.model.UrlPerformanceModel;
import com.jungo.diy.response.UrlPerformanceResponse;
import com.jungo.diy.util.DateUtils;
import com.jungo.diy.util.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Autowired
    private CoreInterfaceConfigMapper coreInterfaceConfigMapper;

    @Transactional(rollbackFor = Exception.class)
    public void writePerformanceData2DB(List<GateWayDailyPerformanceEntity> gateWayDailyPerformanceEntities, List<ApiDailyPerformanceEntity> apiDailyPerformanceEntities) {
        // 判断gateWayDailyPerformanceEntities不为空且不为null
        if (gateWayDailyPerformanceEntities != null && !gateWayDailyPerformanceEntities.isEmpty()) {
            gateWayDailyPerformanceMapper.batchInsert(gateWayDailyPerformanceEntities);
        }
        if (apiDailyPerformanceEntities != null && !apiDailyPerformanceEntities.isEmpty()) {
            // 分批插入apiDailyPerformanceEntities，每批1000条
            for (int i = 0; i < apiDailyPerformanceEntities.size(); i += 1000) {
                List<ApiDailyPerformanceEntity> subList = apiDailyPerformanceEntities.subList(i, Math.min(i + 1000, apiDailyPerformanceEntities.size()));
                apiDailyPerformanceMapper.batchInsert(subList);
            }
        }
    }

    /**
     * 获取指定日期范围内的接口性能数据，并将其封装为UrlPerformanceModel的Map。
     *
     * @param startDate 开始日期，用于获取该天的接口性能数据
     * @param endDate 结束日期，用于获取该天的接口性能数据
     * @return 返回一个Map，其中键为接口的唯一标识符（token），值为对应的UrlPerformanceModel对象
     */
    public Map<String, UrlPerformanceModel> getUrlPerformanceModelMap(LocalDate startDate, LocalDate endDate) {
        // 查询性能数据
        List<ApiDailyPerformanceEntity> startDateEntities = apiDailyPerformanceMapper.findAllByDate(startDate);
        List<ApiDailyPerformanceEntity> endDateEntities = apiDailyPerformanceMapper.findAllByDate(endDate);

        // 将接口性能数据转换成 Map
        Map<String, InterfacePerformanceModel> lastWeekMap = convertToPerformanceMap(startDateEntities);
        Map<String, InterfacePerformanceModel> thisWeekMap = convertToPerformanceMap(endDateEntities);

        // 组装 UrlPerformanceModel 对象并转换成 Map
        return thisWeekMap.entrySet().stream()
                // 仅保留上周也存在的数据
                .filter(entry -> lastWeekMap.containsKey(entry.getKey()))
                .map(entry -> {
                    String token = entry.getKey();
                    InterfacePerformanceModel thisWeek = entry.getValue();
                    InterfacePerformanceModel lastWeek = lastWeekMap.get(token);

                    UrlPerformanceModel urlPerformanceModel = new UrlPerformanceModel();
                    urlPerformanceModel.setToken(token);
                    urlPerformanceModel.setHost(thisWeek.getHost());
                    urlPerformanceModel.setUrl(thisWeek.getUrl());
                    urlPerformanceModel.setLastWeek(lastWeek);
                    urlPerformanceModel.setThisWeek(thisWeek);
                    return urlPerformanceModel;
                })
                .collect(Collectors.toMap(UrlPerformanceModel::getToken, Function.identity()));
    }

    /**
     * 将API性能数据实体列表转换为以Token为key的性能模型Map
     *
     * 注：
     * 1. 使用host和url组合生成唯一Token作为Map的key
     * 2. 对重复key采取保留已有值的策略
     *
     * @param entities API性能数据实体列表
     * @return Map<String, InterfacePerformanceModel>
     *         key: 由host和url生成的Token
     *         value: 包含性能指标数据的模型对象
     */
    private Map<String, InterfacePerformanceModel> convertToPerformanceMap(List<ApiDailyPerformanceEntity> entities) {
        return entities.stream().collect(Collectors.toMap(
                entity -> TokenUtils.generateToken(entity.getHost(), entity.getUrl()),
                entity -> {
                    InterfacePerformanceModel model = new InterfacePerformanceModel();
                    model.setToken(TokenUtils.generateToken(entity.getHost(), entity.getUrl()));
                    model.setHost(entity.getHost());
                    model.setUrl(entity.getUrl());
                    model.setTotalRequestCount(entity.getTotalRequestCount());
                    model.setP99(entity.getP99());
                    model.setP90(entity.getP90());
                    model.setSlowRequestCount(entity.getSlowRequestCount());
                    return model;
                },
                (existing, replacement) -> existing
        ));
    }


    public List<UrlPerformanceResponse> getUrlPerformanceResponses(Integer code, Map<String, UrlPerformanceModel> urlPerformanceModelMap) {
        List<CoreInterfaceConfigEntity> criticalLinkCoreInterface = coreInterfaceConfigMapper.getCoreInterfaceConfigByInterfaceType(code);
        List<UrlPerformanceResponse> criticalLinkUrlPerformanceResponses = new ArrayList<>();
        for (CoreInterfaceConfigEntity coreInterfaceConfigEntity : criticalLinkCoreInterface) {
            String token = TokenUtils.generateToken(coreInterfaceConfigEntity.getHost(), coreInterfaceConfigEntity.getInterfaceUrl());
            UrlPerformanceModel urlPerformanceModel = urlPerformanceModelMap.get(token);
            if (urlPerformanceModel != null) {
                UrlPerformanceResponse urlPerformanceResponse = getUrlPerformanceResponse(urlPerformanceModel, coreInterfaceConfigEntity);
                criticalLinkUrlPerformanceResponses.add(urlPerformanceResponse);
            }
        }
        return criticalLinkUrlPerformanceResponses;
    }

    /**
     * 根据给定的UrlPerformanceModel和CoreInterfaceConfigEntity生成UrlPerformanceResponse对象。
     *
     * @param urlPerformanceModel 包含URL性能数据的模型对象，提供主机、URL、上周和本周的P99、总请求数、慢请求率等信息。
     * @param coreInterfaceConfigEntity 包含核心接口配置的实体对象，提供页面名称、P99目标、接口类型、负责人等信息。
     * @return UrlPerformanceResponse 返回生成的URL性能响应对象，包含从输入模型中提取的性能数据和配置信息。
     */
    private UrlPerformanceResponse getUrlPerformanceResponse(UrlPerformanceModel urlPerformanceModel, CoreInterfaceConfigEntity coreInterfaceConfigEntity) {
        UrlPerformanceResponse urlPerformanceResponse = new UrlPerformanceResponse();
        urlPerformanceResponse.setHost(urlPerformanceModel.getHost());
        urlPerformanceResponse.setPageName(coreInterfaceConfigEntity.getPageName());
        urlPerformanceResponse.setUrl(urlPerformanceModel.getUrl());
        urlPerformanceResponse.setLastWeekP99(urlPerformanceModel.getLastWeek().getP99());
        urlPerformanceResponse.setThisWeekP99(urlPerformanceModel.getThisWeek().getP99());
        urlPerformanceResponse.setLastWeekP90(urlPerformanceModel.getLastWeek().getP90());
        urlPerformanceResponse.setThisWeekP90(urlPerformanceModel.getThisWeek().getP90());
        urlPerformanceResponse.setLastWeekTotalRequestCount(urlPerformanceModel.getLastWeek().getTotalRequestCount());
        urlPerformanceResponse.setThisWeekTotalRequestCount(urlPerformanceModel.getThisWeek().getTotalRequestCount());
        urlPerformanceResponse.setLastWeekSlowRequestRate(urlPerformanceModel.getLastWeek().getSlowRequestRate());
        urlPerformanceResponse.setThisWeekSlowRequestRate(urlPerformanceModel.getThisWeek().getSlowRequestRate());
        urlPerformanceResponse.setP99Change(urlPerformanceModel.getP99Change());
        urlPerformanceResponse.setP99ChangeRate(urlPerformanceModel.getP99ChangeRate());
        urlPerformanceResponse.setP99Target(coreInterfaceConfigEntity.getP99Target());
        urlPerformanceResponse.setOwner(coreInterfaceConfigEntity.getOwner());

        Integer interfaceType = coreInterfaceConfigEntity.getInterfaceType();
        urlPerformanceResponse.setInterfaceType(interfaceType);

        Integer thisWeekP99 = urlPerformanceResponse.getThisWeekP99();
        if (InterfaceTypeEnum.CRITICAL_LINK.getCode().equals(interfaceType)) {
            Integer p99Target = urlPerformanceResponse.getP99Target();
            urlPerformanceResponse.setReachTarget(thisWeekP99 == null || p99Target == null || thisWeekP99 <= p99Target);
        } else {
            // 如果99线变化率大于10%&&99线变化大于30，reachTarget为false。否则是true
            float p99ChangeRate = urlPerformanceResponse.getP99ChangeRate();
            Integer p99Change = urlPerformanceResponse.getP99Change();
            urlPerformanceResponse.setReachTarget(!(p99ChangeRate > 0.1f) || p99Change <= 30);
        }
        return urlPerformanceResponse;
    }

    public List<SlowRequestRateModel> getGatewayAverageSlowRequestRate(int year) {
        List<GateWayDailyPerformanceEntity> gateWayDailyPerformanceEntities = gateWayDailyPerformanceMapper.getPerformanceByYear("cl-gateway.tuhu.cn", LocalDate.of(year, 1, 1));
        // 将gateWayDailyPerformanceEntities按照月份进行分组
        Map<Integer, List<GateWayDailyPerformanceEntity>> map = gateWayDailyPerformanceEntities.stream().collect(Collectors.groupingBy(x -> DateUtils.getMonth(x.getDate())));
        // 计算月慢请求率平均值
        List<SlowRequestRateModel> slowRequestRateModels = new ArrayList<>();
        map.forEach((key, value) -> {
            double average = value.stream()
                    .mapToDouble(GateWayDailyPerformanceEntity::getSlowRequestRate)
                    .average()
                    .orElse(0.0);

            SlowRequestRateModel slowRequestRateModel = new SlowRequestRateModel();
            slowRequestRateModel.setMonth(key);
            slowRequestRateModel.setSlowRequestRate(average);
            slowRequestRateModels.add(slowRequestRateModel);
        });
        // slowRequestRateModels按照key排序
        slowRequestRateModels.sort(Comparator.comparingInt(SlowRequestRateModel::getMonth));
        return slowRequestRateModels;
    }

    public List<GateWayDailyPerformanceEntity> getWeeklyMarketDataSituationTable(LocalDate endDate) {
        if (endDate != null) {
            LocalDate startDate = endDate.minusDays(6);
            List<GateWayDailyPerformanceEntity> apiDailyPerformanceEntities = gateWayDailyPerformanceMapper.getPerformanceByDate(startDate, endDate);
            // apiDailyPerformanceEntities按照date排序
            apiDailyPerformanceEntities.sort(Comparator.comparing(GateWayDailyPerformanceEntity::getDate));
            return apiDailyPerformanceEntities;
        }
        return null;
    }

    public List<GateWayDailyPerformanceEntity> getMonthlySlowRequestRateTrendData(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            List<GateWayDailyPerformanceEntity> apiDailyPerformanceEntities = gateWayDailyPerformanceMapper.getPerformanceByDate(startDate, endDate);
            // apiDailyPerformanceEntities按照date排序
            apiDailyPerformanceEntities.sort(Comparator.comparing(GateWayDailyPerformanceEntity::getDate));
            return apiDailyPerformanceEntities;
        }
        return null;
    }
}
