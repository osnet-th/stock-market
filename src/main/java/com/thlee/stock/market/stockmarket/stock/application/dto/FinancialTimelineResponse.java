package com.thlee.stock.market.stockmarket.stock.application.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 다년 재무 타임라인 응답.
 * 모든 행(row)의 values는 "연도 → 값" 맵이며, 해당 연도에 값이 없으면 키가 없거나 null이다 (0 대체 금지).
 */
@Getter
@Builder
public class FinancialTimelineResponse {

    private final List<TimelineColumn> columns;
    private final List<TimelineRow> summaryAccounts;
    private final List<TimelineIndexGroup> indices;
    private final List<TimelineRow> shares;
    private final TimelineRow fcf;
    private final List<TimelineDetailGroup> details;

    /**
     * 타임라인 컬럼 1개 = 연도별 기준 보고서. partial=true면 진행중(분기/반기 누적) 연도.
     */
    @Getter
    @RequiredArgsConstructor
    public static class TimelineColumn {
        private final String year;
        private final String reportCode;
        private final String reportLabel;
        private final boolean partial;
    }

    @Getter
    @RequiredArgsConstructor
    public static class TimelineRow {
        private final String id;
        private final String name;
        private final Map<String, String> values;
    }

    @Getter
    @RequiredArgsConstructor
    public static class TimelineIndexGroup {
        private final String classCode;
        private final String className;
        private final List<TimelineRow> items;
    }

    @Getter
    @RequiredArgsConstructor
    public static class TimelineDetailGroup {
        private final String statementDiv;
        private final String statementName;
        private final List<TimelineRow> accounts;
    }
}
