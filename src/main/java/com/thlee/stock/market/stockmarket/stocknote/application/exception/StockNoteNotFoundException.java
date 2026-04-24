package com.thlee.stock.market.stockmarket.stocknote.application.exception;

/**
 * 기록이 없거나 현재 사용자 소유가 아닐 때 발생. HTTP 상태는 404 로 매핑 (security 리뷰 권고:
 * IDOR 노출 방지 위해 403 대신 404 통일).
 */
public class StockNoteNotFoundException extends RuntimeException {

    public StockNoteNotFoundException(Long noteId) {
        super("StockNote not found: id=" + noteId);
    }
}