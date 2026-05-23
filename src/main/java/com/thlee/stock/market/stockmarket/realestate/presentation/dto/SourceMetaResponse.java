package com.thlee.stock.market.stockmarket.realestate.presentation.dto;

import java.util.List;

public record SourceMetaResponse(List<SourceEntry> sources) {

    public record SourceEntry(String source,
                              String label,
                              String lastSuccessAt,
                              String nextScheduledAt,
                              long indicatorCount) {
    }
}