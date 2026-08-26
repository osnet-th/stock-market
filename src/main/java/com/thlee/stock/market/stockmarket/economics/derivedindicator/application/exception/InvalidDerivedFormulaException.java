package com.thlee.stock.market.stockmarket.economics.derivedindicator.application.exception;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.service.DerivedFormulaValidator;
import lombok.Getter;

/**
 * 수식 검증 실패 → 400. 안정된 사유 enum만 노출(내부 정보 비노출).
 */
@Getter
public class InvalidDerivedFormulaException extends RuntimeException {

    private final DerivedFormulaValidator.Reason reason;

    public InvalidDerivedFormulaException(DerivedFormulaValidator.Reason reason) {
        super("수식 검증 실패: " + reason);
        this.reason = reason;
    }
}
