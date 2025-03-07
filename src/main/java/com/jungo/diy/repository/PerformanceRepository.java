package com.jungo.diy.repository;

import com.jungo.diy.entity.ApiDailyPerformanceEntity;
import com.jungo.diy.entity.CoreInterfaceConfigEntity;
import com.jungo.diy.entity.GateWayDailyPerformanceEntity;
import com.jungo.diy.enums.InterfaceTypeEnum;
import com.jungo.diy.mapper.ApiDailyPerformanceMapper;
import com.jungo.diy.mapper.CoreInterfaceConfigMapper;
import com.jungo.diy.mapper.GateWayDailyPerformanceMapper;
import com.jungo.diy.model.InterfacePerformanceModel;
import com.jungo.diy.model.UrlPerformanceModel;
import com.jungo.diy.response.UrlPerformanceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
     * 获取指定日期范围内接口性能数据的映射表
     *
     * @param startDate 统计起始日期（上周日期）
     * @param endDate   统计结束日期（本周日期）
     * @return 以接口URL为键，包含本周与上周性能对比数据的UrlPerformanceModel映射表
     */
    public Map<String, UrlPerformanceModel> getUrlPerformanceModelMap(LocalDate startDate, LocalDate endDate) {
        // 获取startDate这天的所有接口性能数据
        List<ApiDailyPerformanceEntity> startDateApiDailyPerformanceEntities = apiDailyPerformanceMapper.findAllByDate(startDate);
        // 获取endDate这天的所有接口性能数据
        List<ApiDailyPerformanceEntity> endDateApiDailyPerformanceEntities = apiDailyPerformanceMapper.findAllByDate(endDate);
        // 上周的接口性能数据
        List<InterfacePerformanceModel> lastWeek = startDateApiDailyPerformanceEntities.stream().map(x -> {
            InterfacePerformanceModel newInterfacePerformanceModel = new InterfacePerformanceModel();
            newInterfacePerformanceModel.setToken(x.getHost() + x.getUrl());
            newInterfacePerformanceModel.setHost(x.getHost());
            newInterfacePerformanceModel.setUrl(x.getUrl());
            newInterfacePerformanceModel.setTotalRequestCount(x.getTotalRequestCount());
            newInterfacePerformanceModel.setP99(x.getP99());
            newInterfacePerformanceModel.setTotalRequestCount(x.getTotalRequestCount());
            newInterfacePerformanceModel.setSlowRequestCount(x.getSlowRequestCount());

            return newInterfacePerformanceModel;
        }).collect(Collectors.toList());
        // 本周的接口性能数据
        List<InterfacePerformanceModel> thisWeek = endDateApiDailyPerformanceEntities.stream().map(x -> {
            InterfacePerformanceModel newInterfacePerformanceModel = new InterfacePerformanceModel();
            newInterfacePerformanceModel.setToken(x.getHost() + x.getUrl());
            newInterfacePerformanceModel.setHost(x.getHost());
            newInterfacePerformanceModel.setUrl(x.getUrl());
            newInterfacePerformanceModel.setTotalRequestCount(x.getTotalRequestCount());
            newInterfacePerformanceModel.setP99(x.getP99());
            newInterfacePerformanceModel.setTotalRequestCount(x.getTotalRequestCount());
            newInterfacePerformanceModel.setSlowRequestCount(x.getSlowRequestCount());
            return newInterfacePerformanceModel;
        }).collect(Collectors.toList());

        // 将lastWeek转换成map
        Map<String, InterfacePerformanceModel> lastWeekMap = lastWeek.stream().collect(Collectors.toMap(InterfacePerformanceModel::getToken, x -> x, (x, y) -> x));
        // 将thisWeek转换成map
        Map<String, InterfacePerformanceModel> thisWeekMap = thisWeek.stream().collect(Collectors.toMap(InterfacePerformanceModel::getToken, x -> x, (x, y) -> x));
        // 组装UrlPerformanceModel对象
        List<UrlPerformanceModel> urlPerformanceModels = new ArrayList<>();
        for (Map.Entry<String, InterfacePerformanceModel> entry : thisWeekMap.entrySet()) {
            String token = entry.getKey();
            InterfacePerformanceModel thisWeekInterfacePerformanceModel = entry.getValue();
            InterfacePerformanceModel lastWeekInterfacePerformanceModel = lastWeekMap.get(token);
            if (Objects.nonNull(lastWeekInterfacePerformanceModel)) {
                UrlPerformanceModel urlPerformanceModel = new UrlPerformanceModel();
                urlPerformanceModel.setToken(token);
                urlPerformanceModel.setHost(thisWeekInterfacePerformanceModel.getHost());
                urlPerformanceModel.setUrl(thisWeekInterfacePerformanceModel.getUrl());
                urlPerformanceModel.setLastWeek(lastWeekInterfacePerformanceModel);
                urlPerformanceModel.setThisWeek(thisWeekInterfacePerformanceModel);
                urlPerformanceModels.add(urlPerformanceModel);
            }

        }

        // urlPerformanceModels转成map
        return urlPerformanceModels.stream().collect(Collectors.toMap(UrlPerformanceModel::getUrl, x -> x, (x, y) -> x));
    }

    public List<UrlPerformanceResponse> getUrlPerformanceResponses(Integer code, Map<String, UrlPerformanceModel> urlPerformanceModelMap) {
        List<CoreInterfaceConfigEntity> criticalLinkCoreInterface = coreInterfaceConfigMapper.getCoreInterfaceConfigByInterfaceType(code);
        List<UrlPerformanceResponse> criticalLinkUrlPerformanceResponses = new ArrayList<>();
        for (CoreInterfaceConfigEntity coreInterfaceConfigEntity : criticalLinkCoreInterface) {
            String url = coreInterfaceConfigEntity.getInterfaceUrl();
            UrlPerformanceModel urlPerformanceModel = urlPerformanceModelMap.get(url);
            if (urlPerformanceModel != null) {
                UrlPerformanceResponse urlPerformanceResponse = getUrlPerformanceResponse(urlPerformanceModel, coreInterfaceConfigEntity);
                criticalLinkUrlPerformanceResponses.add(urlPerformanceResponse);
            }
        }
        return criticalLinkUrlPerformanceResponses;
    }

    private UrlPerformanceResponse getUrlPerformanceResponse(UrlPerformanceModel urlPerformanceModel, CoreInterfaceConfigEntity coreInterfaceConfigEntity) {
        UrlPerformanceResponse urlPerformanceResponse = new UrlPerformanceResponse();
        urlPerformanceResponse.setHost(urlPerformanceModel.getHost());
        urlPerformanceResponse.setPageName(coreInterfaceConfigEntity.getPageName());
        urlPerformanceResponse.setUrl(urlPerformanceModel.getUrl());
        urlPerformanceResponse.setLastWeekP99(urlPerformanceModel.getLastWeek().getP99());
        urlPerformanceResponse.setThisWeekP99(urlPerformanceModel.getThisWeek().getP99());
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
}
