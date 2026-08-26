package com.thlee.stock.market.stockmarket.economics.derivedindicator.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.DerivedFormula;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperand;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperator;

import java.util.List;

/**
 * 수식 응답(편집/표시용 echo).
 */
public record DerivedFormulaResponse(List<Operand> operands, List<FormulaOperator> operators) {

    public record Operand(FormulaOperand.Type type, String className, String keystatName, Double value) {
        static Operand from(FormulaOperand operand) {
            return new Operand(operand.getType(), operand.getClassName(), operand.getKeystatName(), operand.getValue());
        }
    }

    public static DerivedFormulaResponse from(DerivedFormula formula) {
        List<Operand> operands = formula.getOperands().stream().map(Operand::from).toList();
        return new DerivedFormulaResponse(operands, formula.getOperators());
    }
}
