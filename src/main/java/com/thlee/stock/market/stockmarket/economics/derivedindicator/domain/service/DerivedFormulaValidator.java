package com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.service;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.DerivedFormula;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperand;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorCategory;

import java.util.List;
import java.util.Map;

/**
 * 파생지표 수식 생성/수정 시점 검증기. 순수 도메인 서비스 — Spring 의존 없음.
 * <p>
 * 입력 계약(Unit 4가 구성·주입): 사용 가능 지표 메타 맵 {@code Map<compareKey, EcosIndicatorCategory>}
 * (EcosIndicatorMetadataService.getMetadataMap 기반, 캐시 독립 고정 메타).
 * allowCrossCategory=true는 시스템 프리셋 전용(R3 예외) — 사용자 정의는 false.
 */
public class DerivedFormulaValidator {

    private static final int MIN_OPERANDS = 2;
    private static final int MAX_OPERANDS = 3;

    public enum Reason {
        OK,
        INVALID_OPERAND_COUNT,
        INVALID_OPERATOR_COUNT,
        NO_INDICATOR,
        UNKNOWN_INDICATOR,
        CATEGORY_MISMATCH
    }

    /**
     * @param resolvedCategory 모든 지표가 동일 카테고리면 해당 카테고리, 교차(프리셋)면 null(혼합).
     */
    public record ValidationResult(boolean valid, Reason reason, EcosIndicatorCategory resolvedCategory) {

        static ValidationResult fail(Reason reason) {
            return new ValidationResult(false, reason, null);
        }

        static ValidationResult ok(EcosIndicatorCategory resolvedCategory) {
            return new ValidationResult(true, Reason.OK, resolvedCategory);
        }
    }

    public ValidationResult validate(DerivedFormula formula,
                                     Map<String, EcosIndicatorCategory> availableMeta,
                                     boolean allowCrossCategory) {
        List<FormulaOperand> operands = formula.getOperands();
        if (operands.size() < MIN_OPERANDS || operands.size() > MAX_OPERANDS) {
            return ValidationResult.fail(Reason.INVALID_OPERAND_COUNT);
        }
        if (formula.getOperators().size() != operands.size() - 1) {
            return ValidationResult.fail(Reason.INVALID_OPERATOR_COUNT);
        }

        List<FormulaOperand> indicators = formula.indicatorOperands();
        if (indicators.isEmpty()) {
            return ValidationResult.fail(Reason.NO_INDICATOR);
        }

        EcosIndicatorCategory first = null;
        boolean crossed = false;
        for (FormulaOperand indicator : indicators) {
            EcosIndicatorCategory category = availableMeta.get(indicator.compareKey());
            if (category == null) {
                return ValidationResult.fail(Reason.UNKNOWN_INDICATOR);
            }
            if (first == null) {
                first = category;
            } else if (first != category) {
                if (!allowCrossCategory) {
                    return ValidationResult.fail(Reason.CATEGORY_MISMATCH);
                }
                crossed = true; // 교차 카테고리(프리셋) → 혼합
            }
        }

        return ValidationResult.ok(crossed ? null : first);
    }
}
