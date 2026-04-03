package com.thlee.stock.market.stockmarket.overseasnews.infrastructure.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KIS 해외속보(제목) API 응답 output 항목.
 * tr_id: FHKST01011801
 */
@Getter
@NoArgsConstructor
public class KisBreakingNewsOutput {

    @JsonProperty("data_dt")
    private String dataDate;             // 작성일자 (YYYYMMDD)

    @JsonProperty("data_tm")
    private String dataTime;             // 작성시간 (HHMMSS)

    @JsonProperty("hts_pbnt_titl_cntt")
    private String title;                // 제목

    @JsonProperty("dorg")
    private String source;               // 자료원
}