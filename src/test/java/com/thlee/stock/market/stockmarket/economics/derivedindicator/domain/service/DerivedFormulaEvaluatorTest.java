package com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.service;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.DerivedFormula;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperand;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DerivedFormulaEvaluatorTest {

    private final DerivedFormulaEvaluator evaluator = new DerivedFormulaEvaluator();

    private static FormulaOperand ind(String key) {
        return FormulaOperand.indicator("C", key);
    }

    private static String ck(String key) {
        return "C::" + key;
    }

    @Test
    void 두_지표_뺄셈() {
        DerivedFormula f = new DerivedFormula(List.of(ind("A"), ind("B")), List.of(FormulaOperator.SUB));
        var r = evaluator.evaluate(f, Map.of(ck("A"), 5.0, ck("B"), 2.0));
        assertThat(r.computable()).isTrue();
        assertThat(r.value()).isEqualTo(3.0);
    }

    @Test
    void 연산자_우선순위_적용_A_minus_B_div_C() {
        // A - B / C = A - (B/C) = 10 - (6/2) = 7  (좌→우면 (10-6)/2=2 오답)
        DerivedFormula f = new DerivedFormula(
                List.of(ind("A"), ind("B"), ind("C")),
                List.of(FormulaOperator.SUB, FormulaOperator.DIV));
        var r = evaluator.evaluate(f, Map.of(ck("A"), 10.0, ck("B"), 6.0, ck("C"), 2.0));
        assertThat(r.computable()).isTrue();
        assertThat(r.value()).isCloseTo(7.0, within(1e-9));
    }

    @Test
    void 상수_나눗셈() {
        DerivedFormula f = new DerivedFormula(
                List.of(ind("A"), FormulaOperand.constant(100)), List.of(FormulaOperator.DIV));
        var r = evaluator.evaluate(f, Map.of(ck("A"), 250.0));
        assertThat(r.value()).isCloseTo(2.5, within(1e-9));
    }

    @Test
    void 분모_0이면_계산불가() {
        DerivedFormula f = new DerivedFormula(List.of(ind("A"), ind("B")), List.of(FormulaOperator.DIV));
        var r = evaluator.evaluate(f, Map.of(ck("A"), 5.0, ck("B"), 0.0));
        assertThat(r.computable()).isFalse();
        assertThat(r.reason()).isEqualTo(DerivedFormulaEvaluator.Reason.DIVIDE_BY_ZERO);
    }

    @Test
    void 최신값_결측이면_계산불가() {
        DerivedFormula f = new DerivedFormula(List.of(ind("A"), ind("B")), List.of(FormulaOperator.SUB));
        var r = evaluator.evaluate(f, Map.of(ck("A"), 5.0)); // B 없음
        assertThat(r.computable()).isFalse();
        assertThat(r.reason()).isEqualTo(DerivedFormulaEvaluator.Reason.MISSING_VALUE);
    }

    @Test
    void 오버플로_결과는_계산불가_NON_FINITE() {
        DerivedFormula f = new DerivedFormula(List.of(ind("A"), ind("B")), List.of(FormulaOperator.MUL));
        var r = evaluator.evaluate(f, Map.of(ck("A"), Double.MAX_VALUE, ck("B"), 10.0)); // → Infinity
        assertThat(r.computable()).isFalse();
        assertThat(r.reason()).isEqualTo(DerivedFormulaEvaluator.Reason.NON_FINITE);
    }
}
