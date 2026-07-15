package com.thlee.stock.market.stockmarket.companyreport.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot;
import org.springframework.stereotype.Component;

/**
 * ReportSnapshot ↔ JSON 직렬화.
 * 역직렬화는 미지 필드를 허용한다 (schemaVersion 진화 시 구버전 스냅샷도 읽기 위함).
 */
@Component
public class ReportSnapshotJsonMapper {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public String toJson(ReportSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("스냅샷 직렬화 실패", e);
        }
    }

    public ReportSnapshot fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ReportSnapshot.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("스냅샷 역직렬화 실패", e);
        }
    }
}
