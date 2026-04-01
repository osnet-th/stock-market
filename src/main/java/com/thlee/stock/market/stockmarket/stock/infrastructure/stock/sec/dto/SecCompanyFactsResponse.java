package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * SEC EDGAR Company Facts API 응답.
 * 구조: { cik, entityName, facts: { "us-gaap": { "TagName": { units: { "USD": [...] } } } } }
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecCompanyFactsResponse {

    private Long cik;
    private String entityName;
    private Map<String, Map<String, TagData>> facts;

    /**
     * us-gaap 네임스페이스의 태그 데이터를 반환
     */
    public Map<String, TagData> getUsGaapFacts() {
        if (facts == null) {
            return Map.of();
        }
        return facts.getOrDefault("us-gaap", Map.of());
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TagData {
        private String label;
        private String description;
        private Map<String, List<FactEntry>> units;

        public List<FactEntry> getUsdEntries() {
            if (units == null) {
                return List.of();
            }
            return units.getOrDefault("USD", List.of());
        }

        public List<FactEntry> getSharesEntries() {
            if (units == null) {
                return List.of();
            }
            return units.getOrDefault("USD/shares", List.of());
        }
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FactEntry {
        private Double val;
        private String end;
        private String start;
        private String accn;
        private Long fy;
        private String fp;
        private String form;
        private String filed;
        private String frame;
    }
}