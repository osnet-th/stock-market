package com.thlee.stock.market.stockmarket.stocknote.application.dto;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNote;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNotePriceSnapshot;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteTag;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteVerification;

import java.util.List;

/**
 * 기록 상세 조회 결과. note + tags + snapshots (3개) + verification (optional) 을 함께 조립.
 */
public record StockNoteDetailResult(
        StockNote note,
        List<StockNoteTag> tags,
        List<StockNotePriceSnapshot> snapshots,
        StockNoteVerification verification
) { }