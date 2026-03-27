package com.meetr.application.dto;

import lombok.Data;

@Data
public class UpdateFutureSeriesRequest {
    /** 操作人ID */
    private String operatorId;
    /** 新的开始时间（epoch ms），对 seriesIndex >= fromSeriesIndex 的所有预约生效 */
    private Long newStartTimeMs;
    /** 新的结束时间（epoch ms），时长保持不变 */
    private Long newEndTimeMs;
    /** 从第几个实例开始修改（包含），例如 fromSeriesIndex = 3 表示从第3次开始 */
    private Integer fromSeriesIndex;
}
