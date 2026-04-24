package com.thlee.stock.market.stockmarket.stocknote.presentation.dto;

import com.thlee.stock.market.stockmarket.stocknote.application.dto.DashboardResult;

import java.util.List;
import java.util.Map;

/**
 * GET /api/stock-notes/dashboard Response.
 */
public record DashboardResponse(
        long thisMonthCount,
        long verifiedCount,
        long pendingVerificationCount,
        HitRateDto hitRate,
        Map<String, Long> characterDistribution,
        List<TagComboDto> topTagCombinations
) {
    public static DashboardResponse from(DashboardResult r) {
        DashboardResult.HitRate h = r.hitRate();
        return new DashboardResponse(
                r.thisMonthCount(),
                r.verifiedCount(),
                r.pendingVerificationCount(),
                new HitRateDto(h.correct(), h.wrong(), h.partial(), h.total()),
                r.characterDistribution(),
                r.topTagCombinations().stream()
                        .map(e -> new TagComboDto(e.tagValues(), e.count()))
                        .toList()
        );
    }

    public record HitRateDto(long correct, long wrong, long partial, long total) { }

    public record TagComboDto(List<String> tagValues, long count) { }
}