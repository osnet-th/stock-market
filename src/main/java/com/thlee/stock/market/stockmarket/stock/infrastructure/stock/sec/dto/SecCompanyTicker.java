package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * SEC company_tickers.json 파싱 결과.
 * JSON 구조: {"0": {"cik_str": 320193, "ticker": "AAPL", "title": "Apple Inc."}, ...}
 */
@Getter
@NoArgsConstructor
public class SecCompanyTicker {

    @JsonProperty("cik_str")
    private Long cikStr;

    @JsonProperty("ticker")
    private String ticker;

    @JsonProperty("title")
    private String title;
}