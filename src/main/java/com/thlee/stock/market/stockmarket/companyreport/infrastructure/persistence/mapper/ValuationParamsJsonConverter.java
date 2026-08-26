package com.thlee.stock.market.stockmarket.companyreport.infrastructure.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.ValuationParams;
import org.springframework.stereotype.Component;

/**
 * ValuationParams ↔ JSON(jsonb payload) 직렬화 변환기.
 * 폐쇄형 매핑(미지 필드 거부), polymorphic typing 미사용. 손상 JSON은 IllegalStateException으로 전파.
 */
@Component
public class ValuationParamsJsonConverter {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    public String toJson(ValuationParams params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("가치평가 파라미터 직렬화 실패", e);
        }
    }

    public ValuationParams fromJson(String json) {
        try {
            return objectMapper.readValue(json, ValuationParams.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("가치평가 파라미터 역직렬화 실패", e);
        }
    }
}
