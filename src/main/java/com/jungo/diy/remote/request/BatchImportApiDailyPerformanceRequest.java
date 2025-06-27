package com.jungo.diy.remote.request;

import com.jungo.diy.remote.model.ApiDailyPerformanceModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * @author lichuang3
 * @date 2025-06-26 17:56
 */
@Data
public class BatchImportApiDailyPerformanceRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "起始ID", example = "1", required = true)
    private long startId;

    @ApiModelProperty(value = "结束ID", example = "100", required = true)
    private long endId;

    @ApiModelProperty(value = "API每日性能数据列表", required = true)
    @NotEmpty(message = "API性能数据列表不能为空")
    @NotNull(message = "API性能数据列表不能为null")
    private List<ApiDailyPerformanceModel> apiDailyPerformanceModels;
}
