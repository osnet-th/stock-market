package com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.service;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.DerivedFormula;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperand;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 파생지표 평가기. 순수 도메인 서비스 — Spring 의존 없음.
 * <p>
 * 입력: 검증을 통과한 DerivedFormula + 최신값 맵 {@code Map<compareKey, Double>}(application이 변환·주입).
 * 연산자 우선순위 적용(×÷ 먼저, +− 나중). 괄호 없음.
 * 평가 시점 안전 처리: 피연산자 결측/0 나눗셈/NaN/Infinity → "계산 불가"(예외 전파 금지).
 */
public class DerivedFormulaEvaluator {

    public enum Reason {
        OK,
        MISSING_VALUE,
        DIVIDE_BY_ZERO,
        NON_FINITE
    }

    public record EvaluationResult(boolean computable, Double value, Reason reason) {

        static EvaluationResult notComputable(Reason reason) {
            return new EvaluationResult(false, null, reason);
        }

        static EvaluationResult of(double value) {
            if (!Double.isFinite(value)) {
                return notComputable(Reason.NON_FINITE);
            }
            return new EvaluationResult(true, value, Reason.OK);
        }
    }

    public EvaluationResult evaluate(DerivedFormula formula, Map<String, Double> latestValues) {
        List<Double> values = new ArrayList<>();
        for (FormulaOperand operand : formula.getOperands()) {
            Double resolved = resolve(operand, latestValues);
            if (resolved == null || !Double.isFinite(resolved)) {
                return EvaluationResult.notComputable(Reason.MISSING_VALUE);
            }
            values.add(resolved);
        }
        List<FormulaOperator> operators = new ArrayList<>(formula.getOperators());

        // 1차: ×÷ 우선 접기
        FoldResult highPrecedence = fold(values, operators, 2);
        if (highPrecedence.divideByZero) {
            return EvaluationResult.notComputable(Reason.DIVIDE_BY_ZERO);
        }
        // 2차: +− 접기
        FoldResult result = fold(highPrecedence.values, highPrecedence.operators, 1);

        return EvaluationResult.of(result.values.get(0));
    }

    private Double resolve(FormulaOperand operand, Map<String, Double> latestValues) {
        if (operand.isIndicator()) {
            return latestValues.get(operand.compareKey());
        }
        return operand.getValue();
    }

    /**
     * 지정 precedence의 연산자를 좌→우로 접는다. 나머지 연산자/피연산자는 그대로 보존.
     */
    private FoldResult fold(List<Double> values, List<FormulaOperator> operators, int precedence) {
        List<Double> outValues = new ArrayList<>();
        List<FormulaOperator> outOperators = new ArrayList<>();
        outValues.add(values.get(0));

        for (int i = 0; i < operators.size(); i++) {
            FormulaOperator op = operators.get(i);
            double rhs = values.get(i + 1);
            if (op.getPrecedence() == precedence) {
                double lhs = outValues.remove(outValues.size() - 1);
                if (op == FormulaOperator.DIV && rhs == 0.0) {
                    return FoldResult.divByZero();
                }
                outValues.add(apply(op, lhs, rhs));
            } else {
                outOperators.add(op);
                outValues.add(rhs);
            }
        }
        return new FoldResult(outValues, outOperators, false);
    }

    private double apply(FormulaOperator op, double lhs, double rhs) {
        return switch (op) {
            case ADD -> lhs + rhs;
            case SUB -> lhs - rhs;
            case MUL -> lhs * rhs;
            case DIV -> lhs / rhs;
        };
    }

    private record FoldResult(List<Double> values, List<FormulaOperator> operators, boolean divideByZero) {
        static FoldResult divByZero() {
            return new FoldResult(List.of(), List.of(), true);
        }
    }
}
