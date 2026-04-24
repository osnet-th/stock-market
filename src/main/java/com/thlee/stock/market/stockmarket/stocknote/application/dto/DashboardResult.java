package com.thlee.stock.market.stockmarket.stocknote.application.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 대시보드 KPI 집계 결과. Caffeine 캐시 대상이므로 {@link Serializable}.
 */
public record DashboardResult(
        long thisMonthCount,
        long verifiedCount,
        long pendingVerificationCount,
        HitRate hitRate,
        Map<String, Long> characterDistribution,
        List<TagComboEntry> topTagCombinations
) implements Serializable {

    public record HitRate(long correct, long wrong, long partial, long total) implements Serializable { }

    public record TagComboEntry(List<String> tagValues, long count) implements Serializable { }
}