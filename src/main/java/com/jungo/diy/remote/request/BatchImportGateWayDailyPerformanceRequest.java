package com.jungo.diy.remote.request;

import com.jungo.diy.remote.model.GateWayDailyPerformanceModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * @author lichuang3
 * @date 2025-06-27 16:38
 */
@Data
public class BatchImportGateWayDailyPerformanceRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "起始ID", example = "1", required = true)
    private int startId;

    @ApiModelProperty(value = "结束ID", example = "100", required = true)
    private int endId;

    @ApiModelProperty(value = "网关每日性能数据列表", required = true)
    @NotEmpty(message = "网关每日性能数据列表不能为空")
    private List<GateWayDailyPerformanceModel> gateWayDailyPerformanceModels;
}
