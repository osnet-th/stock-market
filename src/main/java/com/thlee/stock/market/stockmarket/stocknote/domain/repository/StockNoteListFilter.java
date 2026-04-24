package com.thlee.stock.market.stockmarket.stocknote.domain.repository;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.JudgmentResult;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.NoteDirection;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.RiseCharacter;

import java.time.LocalDate;

/**
 * 기록 리스트 필터 value. 모든 필드는 nullable (null = 해당 필터 비활성).
 */
public record StockNoteListFilter(
        String stockCode,
        LocalDate fromDate,
        LocalDate toDate,
        NoteDirection direction,
        RiseCharacter character,
        JudgmentResult judgmentResult,
        int offset,
        int limit
) {
    public StockNoteListFilter {
        if (offset < 0) {
            throw new IllegalArgumentException("offset 은 0 이상이어야 합니다.");
        }
        if (limit <= 0 || limit > 200) {
            throw new IllegalArgumentException("limit 은 1~200 범위여야 합니다.");
        }
    }
}