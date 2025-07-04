package com.jungo.diy.service;

import com.jungo.diy.entity.ApiDailyPerformanceEntity;
import com.jungo.diy.entity.CoreInterfaceConfigEntity;
import com.jungo.diy.entity.GateWayDailyPerformanceEntity;
import com.jungo.diy.mapper.ApiDailyPerformanceMapper;
import com.jungo.diy.mapper.CoreInterfaceConfigMapper;
import com.jungo.diy.mapper.GateWayDailyPerformanceMapper;
import com.jungo.diy.remote.model.ApiDailyPerformanceModel;
import com.jungo.diy.remote.model.GateWayDailyPerformanceModel;
import com.jungo.diy.remote.request.BatchImportApiDailyPerformanceRequest;
import com.jungo.diy.remote.request.BatchImportGateWayDailyPerformanceRequest;
import com.jungo.diy.remote.request.CoreInterfaceConfigRequest;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lichuang3
 * @date 2025-06-26 14:55
 */
@Service
@Slf4j
public class DataTransferService {

    @Autowired
    private CoreInterfaceConfigMapper configMapper;

    @Autowired
    private ApiDailyPerformanceMapper apiDailyPerformanceMapper;

    @Autowired
    private GateWayDailyPerformanceMapper gateWayDailyPerformanceMapper;

    public Boolean transferCoreInterfaceConfig() {
        // 首先从数据库中获取核心接口配置
        List<CoreInterfaceConfigEntity> coreInterfaceConfigByInterfaceTypes = new ArrayList<>();
        List<CoreInterfaceConfigEntity> coreInterfaceConfigByInterfaceType1 = configMapper.getCoreInterfaceConfigByInterfaceType(1);
        List<CoreInterfaceConfigEntity> coreInterfaceConfigByInterfaceType2 = configMapper.getCoreInterfaceConfigByInterfaceType(2);
        List<CoreInterfaceConfigEntity> coreInterfaceConfigByInterfaceType3 = configMapper.getCoreInterfaceConfigByInterfaceType(3);
        List<CoreInterfaceConfigEntity> coreInterfaceConfigByInterfaceType4 = configMapper.getCoreInterfaceConfigByInterfaceType(4);
        List<CoreInterfaceConfigEntity> coreInterfaceConfigByInterfaceType5 = configMapper.getCoreInterfaceConfigByInterfaceType(5);
        coreInterfaceConfigByInterfaceTypes.addAll(coreInterfaceConfigByInterfaceType1);
        coreInterfaceConfigByInterfaceTypes.addAll(coreInterfaceConfigByInterfaceType2);
        coreInterfaceConfigByInterfaceTypes.addAll(coreInterfaceConfigByInterfaceType3);
        coreInterfaceConfigByInterfaceTypes.addAll(coreInterfaceConfigByInterfaceType4);
        coreInterfaceConfigByInterfaceTypes.addAll(coreInterfaceConfigByInterfaceType5);

        // 按interfaceType升序排序，再按sortOrder升序排序
        List<CoreInterfaceConfigEntity> sortedList = coreInterfaceConfigByInterfaceTypes.stream()
                .sorted(Comparator.comparing(CoreInterfaceConfigEntity::getInterfaceType)
                        .thenComparing(CoreInterfaceConfigEntity::getSortOrder))
                .collect(Collectors.toList());

        // 转化成远程接口入参
        List<CoreInterfaceConfigRequest> coreInterfaceConfigRequests = convert2CoreInterfaceConfigRequests(sortedList);

        // 使用Unirest调用远程接口
        try {
            HttpResponse<JsonNode> response = Unirest.post("http://localhost:9000/api/configs/batchSave")
                    .header("Content-Type", "application/json")
                    .body(coreInterfaceConfigRequests)  // 自动序列化为JSON
                    .asJson();

            if (response.getStatus() == 200) {
                log.info("接口调用成功: {}", response.getBody());
                return true;
            } else {
                log.error("接口调用失败: {} - {}", response.getStatus(), response.getStatusText());
                return false;
            }
        } catch (UnirestException e) {
            log.error("调用远程接口异常", e);
            return false;
        }

    }

    private List<CoreInterfaceConfigRequest> convert2CoreInterfaceConfigRequests(List<CoreInterfaceConfigEntity> coreInterfaceConfigEntities) {
        if (CollectionUtils.isEmpty(coreInterfaceConfigEntities)) {
            return Collections.emptyList();
        }

        return coreInterfaceConfigEntities.stream()
                .map(configEntity -> {
                    CoreInterfaceConfigRequest request = new CoreInterfaceConfigRequest();
                    request.setPageName(configEntity.getPageName());
                    request.setInterfaceUrl(configEntity.getInterfaceUrl());
                    request.setP99Target(configEntity.getP99Target());
                    request.setSlowRequestRateTarget(configEntity.getSlowRequestRateTarget());
                    request.setInterfaceType(configEntity.getInterfaceType());
                    request.setSortOrder(configEntity.getSortOrder());
                    request.setOwner(configEntity.getOwner());
                    request.setHost(configEntity.getHost());
                    return request;
                })
                .collect(Collectors.toList());
    }

    public Boolean transferApiDailyPerformance() {

        int start = 752501;
        int end = 798177;
        int batchSize = 1000;
        for (int currentStart = start; currentStart <= end; currentStart += batchSize) {
            int currentEnd = Math.min(currentStart + batchSize - 1, end);

            try {
                // 添加1秒休眠
                Thread.sleep(1000);
                List<ApiDailyPerformanceEntity> batchRecords = apiDailyPerformanceMapper.getRecordsByPkidRange(currentStart, currentEnd)
                        .stream().filter(x -> {
                            String url = x.getUrl();
                            int byteLength = url.getBytes(StandardCharsets.UTF_8).length;
                            return byteLength <= 200;
                        }).collect(Collectors.toList());

                if (batchRecords.isEmpty()) {
                    continue;
                }

                BatchImportApiDailyPerformanceRequest request = convert2BatchImportApiDailyPerformanceRequest(batchRecords, currentStart, currentEnd);

                HttpResponse<JsonNode> response = Unirest.post("http://localhost:9000/api/dailyPerformance/batchImport")
                        .header("Content-Type", "application/json")
                        .body(request)
                        .asJson();

                if (response.getStatus() != 200) {
                    log.error("批次{}-{}处理失败: {} - {}", currentStart, currentEnd, response.getStatus(), response.getStatusText());
                    return false;
                }

                log.info("批次{}-{}处理成功", currentStart, currentEnd);
            } catch (Exception e) {
                log.error("批次{}-{}处理异常", currentStart, currentEnd, e);
                return false;
            }
        }

        return true;
    }

    private BatchImportApiDailyPerformanceRequest convert2BatchImportApiDailyPerformanceRequest(List<ApiDailyPerformanceEntity> recordsByPkidRange,
                                                                                                long startId,
                                                                                                long endId) {
        BatchImportApiDailyPerformanceRequest request = new BatchImportApiDailyPerformanceRequest();
        request.setStartId(startId);
        request.setEndId(endId);

        if (CollectionUtils.isEmpty(recordsByPkidRange)) {
            request.setApiDailyPerformanceModels(Collections.emptyList());
            return request;
        }

        List<ApiDailyPerformanceModel> dataList = recordsByPkidRange.stream()
                .map(entity -> {
                    ApiDailyPerformanceModel data = new ApiDailyPerformanceModel();
                    data.setHost(entity.getHost());
                    data.setUrl(entity.getUrl());
                    data.setP99(entity.getP99());
                    data.setP999(entity.getP999());
                    data.setP90(entity.getP90());
                    data.setP75(entity.getP75());
                    data.setP50(entity.getP50());
                    data.setP95(entity.getP95());
                    data.setTotalRequestCount(entity.getTotalRequestCount());
                    data.setSlowRequestCount(entity.getSlowRequestCount());
                    data.setDate(entity.getDate());
                    return data;
                })
                .collect(Collectors.toList());

        request.setApiDailyPerformanceModels(dataList);
        return request;
    }

    public Boolean transferGateWayDailyPerformance() {
        // 根据实际业务调整起始ID
        int start = 692;
        // 根据实际业务调整结束ID
        int end = 706;
        // 每批次处理数量
        int batchSize = 500;

        for (int currentStart = start; currentStart <= end; currentStart += batchSize) {
            int currentEnd = Math.min(currentStart + batchSize - 1, end);

            try {
                // 适当休眠避免压力过大
                Thread.sleep(1000);

                // 批量查询数据并过滤
                List<GateWayDailyPerformanceEntity> batchRecords = gateWayDailyPerformanceMapper.getRecordsByPkidRange(currentStart, currentEnd);

                if (batchRecords.isEmpty()) {
                    continue;
                }

                // 构建并发送请求
                BatchImportGateWayDailyPerformanceRequest request = convert2BatchImportGateWayDailyPerformanceRequest(
                        batchRecords, currentStart, currentEnd);

                HttpResponse<JsonNode> response = Unirest.post("http://localhost:9000/api/gateWayDailyPerformance/batchImport")
                        .header("Content-Type", "application/json")
                        .body(request)
                        .asJson();

                if (response.getStatus() != 200) {
                    log.error("网关性能数据导入失败 {}-{}: {}", currentStart, currentEnd, response.getStatusText());
                    return false;
                }

                log.info("成功处理网关性能数据 {}-{}", currentStart, currentEnd);
            } catch (Exception e) {
                log.error("处理网关性能数据异常 {}-{}", currentStart, currentEnd, e);
                return false;
            }
        }
        return true;
    }

    private BatchImportGateWayDailyPerformanceRequest convert2BatchImportGateWayDailyPerformanceRequest(List<GateWayDailyPerformanceEntity> batchRecords,
                                                                                                        int currentStart,
                                                                                                        int currentEnd) {
        BatchImportGateWayDailyPerformanceRequest request = new BatchImportGateWayDailyPerformanceRequest();
        request.setStartId(currentStart);
        request.setEndId(currentEnd);

        if (CollectionUtils.isEmpty(batchRecords)) {
            request.setGateWayDailyPerformanceModels(Collections.emptyList());
            return request;
        }

        List<GateWayDailyPerformanceModel> dataList = batchRecords.stream()
                .map(entity -> {
                    GateWayDailyPerformanceModel data = new GateWayDailyPerformanceModel();
                    data.setHost(entity.getHost());
                    data.setP99(entity.getP99());
                    data.setP999(entity.getP999());
                    data.setP90(entity.getP90());
                    data.setP75(entity.getP75());
                    data.setP50(entity.getP50());
                    data.setTotalRequestCount(entity.getTotalRequestCount());
                    data.setSlowRequestCount(entity.getSlowRequestCount());
                    data.setDate(entity.getDate());
                    return data;
                })
                .collect(Collectors.toList());

        request.setGateWayDailyPerformanceModels(dataList);
        return request;
    }
}
