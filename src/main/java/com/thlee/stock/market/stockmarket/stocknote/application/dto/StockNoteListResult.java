package com.thlee.stock.market.stockmarket.stocknote.application.dto;

import java.util.List;

/**
 * 기록 리스트 페이징 결과.
 */
public record StockNoteListResult(
        List<StockNoteListItemResult> items,
        long totalCount,
        int page,
        int size
) { }