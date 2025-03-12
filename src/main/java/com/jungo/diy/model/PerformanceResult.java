
package com.jungo.diy.model;

import com.jungo.diy.entity.GateWayDailyPerformanceEntity;
import com.jungo.diy.response.UrlPerformanceResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 性能结果类，包含网关和URL性能的统计信息
 * @author lichuang3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceResult {

    /**
     * 各网关的平均慢请求速率列表
     */
    private List<SlowRequestRateModel> gatewayAverageSlowRequestRate;

    /**
     * 过去一周的平均慢请求速率
     */
    private double averageSlowRequestRateInThePastWeek;

    /**
     * 每周市场数据情况数据列表
     */
    private List<GateWayDailyPerformanceEntity> weeklyMarketDataSituationData;

    /**
     * 月度慢请求速率趋势数据列表
     */
    private List<GateWayDailyPerformanceEntity> monthlySlowRequestRateTrendData;

    /**
     * 关键链路URL性能响应列表
     */
    private List<UrlPerformanceResponse> criticalLinkUrlPerformanceResponses;

    /**
     * 五大金刚URL性能响应列表
     */
    private List<UrlPerformanceResponse> fiveGangJingUrlPerformanceResponses;

    /**
     * 首屏标签URL性能响应列表
     */
    private List<UrlPerformanceResponse> firstScreenTabUrlPerformanceResponses;

    /**
     * 麒麟组件接口URL性能响应列表
     */
    private List<UrlPerformanceResponse> qilinComponentInterfaceUrlPerformanceResponses;

    /**
     * 其他核心业务接口URL性能响应列表
     */
    private List<UrlPerformanceResponse> otherCoreBusinessInterfaceUrlPerformanceResponses;

    /**
     * 访问量Top30的接口列表
     */
    private List<UrlPerformanceResponse> accessVolumeTop30Interface;

}
