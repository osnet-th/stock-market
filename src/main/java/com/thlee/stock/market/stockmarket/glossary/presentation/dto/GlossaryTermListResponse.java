package com.thlee.stock.market.stockmarket.glossary.presentation.dto;

import com.thlee.stock.market.stockmarket.glossary.application.dto.GlossaryTermListResult;

import java.util.List;

public record GlossaryTermListResponse(
        List<GlossaryTermResponse> items,
        long totalCount,
        int page,
        int size
) {
    public static GlossaryTermListResponse from(GlossaryTermListResult r) {
        return new GlossaryTermListResponse(
                r.items().stream().map(GlossaryTermResponse::from).toList(),
                r.totalCount(), r.page(), r.size()
        );
    }
}