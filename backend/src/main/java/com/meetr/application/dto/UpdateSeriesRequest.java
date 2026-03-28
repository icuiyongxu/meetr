package com.meetr.application.dto;

import com.meetr.domain.enums.SeriesScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateSeriesRequest {

    @NotBlank
    private String operatorId;

    @NotNull
    private SeriesScope scope;

    /** 可选：新的主题（不填则保持原值） */
    private String subject;

    /** 新的开始时间（epoch ms），不填则保持原值 */
    private Long startTime;

    /** 新的结束时间（epoch ms），不填则保持原值 */
    private Long endTime;

    /** 新的参会人数，不填则保持原值 */
    private Integer attendeeCount;

    /** 新的备注，不填则保持原值 */
    private String remark;
}
