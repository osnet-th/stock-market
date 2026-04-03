package com.thlee.stock.market.stockmarket.overseasnews.infrastructure.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KIS 해외뉴스종합(제목) API 응답 output 항목.
 * tr_id: HHPSTH60100C1
 */
@Getter
@NoArgsConstructor
public class KisNewsOutput {

    @JsonProperty("data_dt")
    private String dataDate;             // 조회일자 (YYYYMMDD)

    @JsonProperty("data_tm")
    private String dataTime;             // 조회시간 (HHMMSS)

    @JsonProperty("title")
    private String title;                // 제목

    @JsonProperty("class_name")
    private String className;            // 중분류명

    @JsonProperty("source")
    private String source;               // 자료원

    @JsonProperty("symb_name")
    private String stockName;            // 종목명
}