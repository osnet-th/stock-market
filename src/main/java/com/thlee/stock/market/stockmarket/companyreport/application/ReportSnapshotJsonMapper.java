package com.thlee.stock.market.stockmarket.companyreport.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;

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
            return withDefaults(objectMapper.readValue(json, ReportSnapshot.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("스냅샷 역직렬화 실패", e);
        }
    }

    /**
     * v1(국내 전용) 스냅샷 하위 호환 — country/currency/unsupportedSections 부재 시 KR/KRW/빈 목록으로 해석
     */
    private ReportSnapshot withDefaults(ReportSnapshot snapshot) {
        if (snapshot.country() != null && snapshot.currency() != null && snapshot.unsupportedSections() != null) {
            return snapshot;
        }
        return new ReportSnapshot(
                snapshot.schemaVersion(),
                snapshot.stockCode(),
                snapshot.stockName(),
                snapshot.country() != null ? snapshot.country() : ReportSnapshot.COUNTRY_KR,
                snapshot.currency() != null ? snapshot.currency() : ReportSnapshot.CURRENCY_KRW,
                snapshot.unsupportedSections() != null ? snapshot.unsupportedSections() : List.of(),
                snapshot.fsDiv(),
                snapshot.companyProfile(),
                snapshot.columns(),
                snapshot.performance(),
                snapshot.statements(),
                snapshot.ratios(),
                snapshot.priceMetrics(),
                snapshot.valuationInputs(),
                snapshot.shareholders(),
                snapshot.riskSignals());
    }
}
