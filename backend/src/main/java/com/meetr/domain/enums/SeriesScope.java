package com.meetr.domain.enums;

/**
 * 系列预约操作范围。
 */
public enum SeriesScope {
    /**
     * 仅本次（当前一条）
     */
    ONCE,
    /**
     * 本次及后续所有实例
     */
    FUTURE,
    /**
     * 整个系列全部实例
     */
    ALL
}
