package com.thlee.stock.market.stockmarket.stocknote.application.dto;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNote;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNotePriceSnapshot;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteTag;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteVerification;

import java.util.List;

/**
 * 리스트 화면에서 한 기록을 요약하는 결과.
 *
 * <p>아이템별 tags (고정 요약 + custom 소수), 스냅샷 3종, 검증 여부(verification nullable) 를 동봉.
 * 리스트 화면에서 카드 렌더에 필요한 정보를 N+1 없이 한 번에 반환.
 */
public record StockNoteListItemResult(
        StockNote note,
        List<StockNoteTag> tags,
        List<StockNotePriceSnapshot> snapshots,
        StockNoteVerification verification
) { }