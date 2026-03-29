package com.meetr.application.dto;

import lombok.Data;

/**
 * 预约搜索请求。
 * 所有字段为可选，null 表示不过滤。
 * 时间范围按北京时区解读。
 */
@Data
public class BookingSearchRequest {

    /** 模糊匹配主题 */
    private String keyword;

    /** 预约人 ID（精确） */
    private String bookerId;

    /** 会议室 ID（精确） */
    private Long roomId;

    /** 状态（精确） */
    private String status;

    /** 审批状态（精确） */
    private String approvalStatus;

    /** 开始时间下限（UTC ms，含） */
    private Long startTimeFrom;

    /** 开始时间上限（UTC ms，含） */
    private Long startTimeTo;

    /** 每页大小，默认 10 */
    private int page = 0;

    /** 页码，默认 0 */
    private int size = 10;
}
