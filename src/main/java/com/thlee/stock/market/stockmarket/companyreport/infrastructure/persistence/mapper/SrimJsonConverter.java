package com.thlee.stock.market.stockmarket.companyreport.infrastructure.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.SrimData;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.SrimValuation;
import org.springframework.stereotype.Component;

/**
 * SrimValuation ↔ JSON(jsonb payload) 직렬화 변환기.
 * 역직렬화는 미지 필드를 허용한다 (schemaVersion 진화 시 구버전 앱도 기존 행을 읽기 위함).
 * 이 변환기는 목록 조회 경로에서도 호출되므로, 한 행의 해독 실패가 목록 전체를 막지 않도록 관대한 정책을 택한다.
 */
@Component
public class SrimJsonConverter {
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public String toJson(SrimValuation value) {
        if (value == null) return null;
        try { return mapper.writeValueAsString(SrimData.from(value)); }
        catch (JsonProcessingException e) { throw new IllegalStateException("S-RIM 직렬화 실패", e); }
    }

    public SrimValuation fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try { return mapper.readValue(json, SrimData.class).toDomain(); }
        catch (JsonProcessingException e) { throw new IllegalStateException("S-RIM 역직렬화 실패", e); }
    }
}
