package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * SEC EDGAR Submissions API 응답 (기업 프로필 + 최근 제출 서식 목록).
 * 구조: { name, sic, sicDescription, fiscalYearEnd, addresses: { business: {...} }, filings: { recent: { 병렬 배열 } } }
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecSubmissionsResponse {

    private String name;
    private String sic;
    private String sicDescription;
    private String fiscalYearEnd;
    private String website;
    private String investorWebsite;
    private List<String> exchanges;
    private List<String> tickers;
    private Map<String, Address> addresses;
    private Filings filings;

    public Address getBusinessAddress() {
        if (addresses == null) {
            return null;
        }
        return addresses.get("business");
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Address {
        private String street1;
        private String street2;
        private String city;
        private String stateOrCountry;
        private String zipCode;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Filings {
        private Recent recent;
    }

    /**
     * 최근 제출 서식 — 같은 인덱스끼리 한 건을 이루는 병렬 배열
     */
    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Recent {
        private List<String> accessionNumber;
        private List<String> filingDate;
        private List<String> form;
        private List<String> primaryDocument;
        private List<String> primaryDocDescription;
    }
}
