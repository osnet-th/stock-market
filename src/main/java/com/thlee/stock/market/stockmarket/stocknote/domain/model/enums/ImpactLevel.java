package com.thlee.stock.market.stockmarket.stocknote.domain.model.enums;

/**
 * 실적/현금흐름 영향도 3단계.
 * StockNote 의 매출·영업이익·현금흐름 영향도 컬럼에 사용된다.
 *
 * <p><b>중요: Enum 상수 이름은 재명명 금지.</b>
 */
public enum ImpactLevel {
    HIGH,
    MEDIUM,
    LOW
}