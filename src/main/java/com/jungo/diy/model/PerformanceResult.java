package com.jungo.diy.model;

import com.jungo.diy.response.UrlPerformanceResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author lichuang3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceResult {
    private List<UrlPerformanceResponse> criticalLinkUrlPerformanceResponses;
    private List<UrlPerformanceResponse> fiveGangJingUrlPerformanceResponses;
    private List<UrlPerformanceResponse> firstScreenTabUrlPerformanceResponses;
    private List<UrlPerformanceResponse> qilinComponentInterfaceUrlPerformanceResponses;
    private List<UrlPerformanceResponse> otherCoreBusinessInterfaceUrlPerformanceResponses;
    private List<UrlPerformanceResponse> accessVolumeTop30Interface;

}