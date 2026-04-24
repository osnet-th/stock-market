package com.thlee.stock.market.stockmarket.stocknote.application.exception;

/**
 * 사후 검증이 이미 존재해 본문이 잠긴 상태에서 수정을 시도할 때 발생. HTTP 409 Conflict 로 매핑.
 */
public class StockNoteLockedException extends RuntimeException {

    public static final String ERROR_CODE = "LOCKED_BY_VERIFICATION";

    public StockNoteLockedException(Long noteId) {
        super("StockNote is locked by verification: id=" + noteId);
    }
}