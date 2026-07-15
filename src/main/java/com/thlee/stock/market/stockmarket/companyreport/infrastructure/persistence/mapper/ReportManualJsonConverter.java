package com.thlee.stock.market.stockmarket.companyreport.infrastructure.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.ReportManual;
import org.springframework.stereotype.Component;

/**
 * ReportManual ↔ JSON(jsonb payload) 직렬화 변환기.
 * 역직렬화는 미지 필드를 허용한다 (schemaVersion 진화 시 구버전 행도 읽기 위함).
 */
@Component
public class ReportManualJsonConverter {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public String toJson(ReportManual manual) {
        try {
            return objectMapper.writeValueAsString(manual);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("정성 입력 직렬화 실패", e);
        }
    }

    public ReportManual fromJson(String json) {
        if (json == null || json.isBlank()) {
            return ReportManual.empty();
        }
        try {
            return objectMapper.readValue(json, ReportManual.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("정성 입력 역직렬화 실패", e);
        }
    }
}
