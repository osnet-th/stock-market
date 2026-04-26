package com.thlee.stock.market.stockmarket.stocknote.domain.model.enums;

/**
 * 사후 검증 결과. 사용자가 수동으로 판정한다.
 *
 * <p><b>중요: Enum 상수 이름은 재명명 금지.</b>
 */
public enum JudgmentResult {
    CORRECT,   // 적중
    WRONG,     // 오판
    PARTIAL    // 부분 적중
}