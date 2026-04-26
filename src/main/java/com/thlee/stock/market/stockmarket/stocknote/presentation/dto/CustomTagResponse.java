package com.thlee.stock.market.stockmarket.stocknote.presentation.dto;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteCustomTag;

import java.util.List;

/**
 * GET /api/stock-notes/custom-tags Response.
 */
public record CustomTagResponse(List<Item> items) {

    public static CustomTagResponse from(List<StockNoteCustomTag> tags) {
        return new CustomTagResponse(tags.stream()
                .map(t -> new Item(t.getTagValue(), t.getUsageCount()))
                .toList());
    }

    public record Item(String tagValue, long usageCount) { }
}