package com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.service;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.DerivedFormula;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperand;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperator;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorCategory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DerivedFormulaValidatorTest {

    private final DerivedFormulaValidator validator = new DerivedFormulaValidator();

    private static final Map<String, EcosIndicatorCategory> META = Map.of(
            "시장금리::A", EcosIndicatorCategory.INTEREST_RATE,
            "시장금리::B", EcosIndicatorCategory.INTEREST_RATE,
            "통화량::C", EcosIndicatorCategory.MONEY_FINANCE
    );

    private static FormulaOperand ind(String className, String keystatName) {
        return FormulaOperand.indicator(className, keystatName);
    }

    @Test
    void 동일_카테고리_2항_통과() {
        DerivedFormula f = new DerivedFormula(
                List.of(ind("시장금리", "A"), ind("시장금리", "B")), List.of(FormulaOperator.SUB));
        DerivedFormulaValidator.ValidationResult r = validator.validate(f, META, false);
        assertThat(r.valid()).isTrue();
        assertThat(r.resolvedCategory()).isEqualTo(EcosIndicatorCategory.INTEREST_RATE);
    }

    @Test
    void 동일_카테고리_3항_상수포함_통과() {
        DerivedFormula f = new DerivedFormula(
                List.of(ind("시장금리", "A"), ind("시장금리", "B"), FormulaOperand.constant(100)),
                List.of(FormulaOperator.SUB, FormulaOperator.MUL));
        assertThat(validator.validate(f, META, false).valid()).isTrue();
    }

    @Test
    void 피연산자_1개면_거부() {
        DerivedFormula f = new DerivedFormula(List.of(ind("시장금리", "A")), List.of());
        assertThat(validator.validate(f, META, false).reason())
                .isEqualTo(DerivedFormulaValidator.Reason.INVALID_OPERAND_COUNT);
    }

    @Test
    void 존재하지_않는_지표_거부() {
        DerivedFormula f = new DerivedFormula(
                List.of(ind("시장금리", "A"), ind("시장금리", "ZZZ")), List.of(FormulaOperator.SUB));
        assertThat(validator.validate(f, META, false).reason())
                .isEqualTo(DerivedFormulaValidator.Reason.UNKNOWN_INDICATOR);
    }

    @Test
    void 사용자정의_교차_카테고리_거부() {
        DerivedFormula f = new DerivedFormula(
                List.of(ind("시장금리", "A"), ind("통화량", "C")), List.of(FormulaOperator.DIV));
        assertThat(validator.validate(f, META, false).reason())
                .isEqualTo(DerivedFormulaValidator.Reason.CATEGORY_MISMATCH);
    }

    @Test
    void 프리셋_교차_카테고리_허용() {
        DerivedFormula f = new DerivedFormula(
                List.of(ind("시장금리", "A"), ind("통화량", "C")), List.of(FormulaOperator.DIV));
        DerivedFormulaValidator.ValidationResult r = validator.validate(f, META, true);
        assertThat(r.valid()).isTrue();
        assertThat(r.resolvedCategory()).isNull(); // 혼합
    }

    @Test
    void operators_개수_불일치_거부() {
        DerivedFormula f = new DerivedFormula(
                List.of(ind("시장금리", "A"), ind("시장금리", "B")),
                List.of(FormulaOperator.SUB, FormulaOperator.ADD));
        assertThat(validator.validate(f, META, false).reason())
                .isEqualTo(DerivedFormulaValidator.Reason.INVALID_OPERATOR_COUNT);
    }
}
