package com.thlee.stock.market.stockmarket.stocknote.domain.exception;

/**
 * 사후 검증이 이미 존재해 본문이 잠긴 상태에서 수정을 시도할 때 발생.
 * 비즈니스 규칙 위반이라 domain.exception 위치 (ARCHITECTURE.md Section 5).
 * presentation 에서 HTTP 409 Conflict 로 매핑.
 */
public class StockNoteLockedException extends RuntimeException {

    public static final String ERROR_CODE = "LOCKED_BY_VERIFICATION";

    public StockNoteLockedException(Long noteId) {
        super("StockNote is locked by verification: id=" + noteId);
    }
}
