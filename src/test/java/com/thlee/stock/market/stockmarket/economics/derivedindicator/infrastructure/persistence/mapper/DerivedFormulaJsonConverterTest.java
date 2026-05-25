package com.thlee.stock.market.stockmarket.economics.derivedindicator.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.DerivedFormula;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperand;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DerivedFormulaJsonConverterTest {

    private final DerivedFormulaJsonConverter converter = new DerivedFormulaJsonConverter();

    @Test
    void 두_지표_수식_round_trip() {
        DerivedFormula original = new DerivedFormula(
                List.of(FormulaOperand.indicator("시장금리", "국고채수익률(5년)"),
                        FormulaOperand.indicator("시장금리", "CD수익률(91일)")),
                List.of(FormulaOperator.SUB));

        DerivedFormula restored = converter.fromJson(converter.toJson(original));

        assertThat(restored.getOperands()).hasSize(2);
        assertThat(restored.getOperators()).containsExactly(FormulaOperator.SUB);
        assertThat(restored.getOperands().get(0).getClassName()).isEqualTo("시장금리");
        assertThat(restored.getOperands().get(0).getKeystatName()).isEqualTo("국고채수익률(5년)");
    }

    @Test
    void 상수_포함_3항_round_trip() {
        DerivedFormula original = new DerivedFormula(
                List.of(FormulaOperand.indicator("시장금리", "국고채수익률(5년)"),
                        FormulaOperand.indicator("시장금리", "CD수익률(91일)"),
                        FormulaOperand.constant(100)),
                List.of(FormulaOperator.SUB, FormulaOperator.MUL));

        DerivedFormula restored = converter.fromJson(converter.toJson(original));

        assertThat(restored.getOperands()).hasSize(3);
        FormulaOperand constant = restored.getOperands().get(2);
        assertThat(constant.getType()).isEqualTo(FormulaOperand.Type.CONSTANT);
        assertThat(constant.getValue()).isEqualTo(100.0);
        assertThat(restored.getOperators()).containsExactly(FormulaOperator.SUB, FormulaOperator.MUL);
    }

    @Test
    void 손상_JSON은_FormulaDeserializationException() {
        assertThatThrownBy(() -> converter.fromJson("{not valid json"))
                .isInstanceOf(DerivedFormulaJsonConverter.FormulaDeserializationException.class);
    }

    @Test
    void 알수없는_연산자는_거부() {
        String json = "{\"operands\":[{\"type\":\"INDICATOR\",\"className\":\"시장금리\",\"keystatName\":\"A\"},"
                + "{\"type\":\"INDICATOR\",\"className\":\"시장금리\",\"keystatName\":\"B\"}],\"operators\":[\"POW\"]}";
        assertThatThrownBy(() -> converter.fromJson(json))
                .isInstanceOf(DerivedFormulaJsonConverter.FormulaDeserializationException.class);
    }

    @Test
    void 미지_필드는_거부() {
        String json = "{\"operands\":[{\"type\":\"INDICATOR\",\"className\":\"시장금리\",\"keystatName\":\"A\"},"
                + "{\"type\":\"INDICATOR\",\"className\":\"시장금리\",\"keystatName\":\"B\"}],"
                + "\"operators\":[\"SUB\"],\"evil\":\"x\"}";
        assertThatThrownBy(() -> converter.fromJson(json))
                .isInstanceOf(DerivedFormulaJsonConverter.FormulaDeserializationException.class);
    }
}
