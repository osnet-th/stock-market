package com.thlee.stock.market.stockmarket.economics.derivedindicator.infrastructure.persistence.mapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.DerivedFormula;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperand;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DerivedFormula ↔ JSON(jsonb payload) 직렬화 변환기.
 * <p>
 * 폐쇄형(closed) 매핑: FormulaOperator/operand type enum으로만 역직렬화, 미지 필드 거부.
 * polymorphic/default typing 미사용 — 임의 클래스 역직렬화 표면 없음.
 * 손상/비호환 JSON은 {@link FormulaDeserializationException}으로 전파(상위에서 graceful 처리).
 */
@Component
public class DerivedFormulaJsonConverter {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    public String toJson(DerivedFormula formula) {
        try {
            FormulaJson dto = new FormulaJson(
                    formula.getOperands().stream().map(OperandJson::from).toList(),
                    formula.getOperators().stream().map(Enum::name).toList()
            );
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new FormulaDeserializationException("파생지표 수식 직렬화 실패", e);
        }
    }

    public DerivedFormula fromJson(String json) {
        try {
            FormulaJson dto = objectMapper.readValue(json, FormulaJson.class);
            List<FormulaOperand> operands = dto.operands().stream().map(OperandJson::toDomain).toList();
            List<FormulaOperator> operators = dto.operators().stream().map(FormulaOperator::valueOf).toList();
            return new DerivedFormula(operands, operators);
        } catch (JsonProcessingException | IllegalArgumentException | NullPointerException e) {
            throw new FormulaDeserializationException("파생지표 수식 역직렬화 실패", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    private record FormulaJson(List<OperandJson> operands, List<String> operators) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    private record OperandJson(String type, String className, String keystatName, Double value) {

        static OperandJson from(FormulaOperand operand) {
            return new OperandJson(
                    operand.getType().name(),
                    operand.getClassName(),
                    operand.getKeystatName(),
                    operand.getValue()
            );
        }

        FormulaOperand toDomain() {
            FormulaOperand.Type parsedType = FormulaOperand.Type.valueOf(type);
            return switch (parsedType) {
                case INDICATOR -> FormulaOperand.indicator(className, keystatName);
                case CONSTANT -> FormulaOperand.constant(value);
            };
        }
    }

    public static class FormulaDeserializationException extends RuntimeException {
        public FormulaDeserializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
