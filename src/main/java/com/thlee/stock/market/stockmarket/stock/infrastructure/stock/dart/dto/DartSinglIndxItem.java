package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DartSinglIndxItem {

    @JsonProperty("reprt_code")
    private String reprtCode;

    @JsonProperty("bsns_year")
    private String bsnsYear;

    @JsonProperty("corp_code")
    private String corpCode;

    @JsonProperty("stock_code")
    private String stockCode;

    @JsonProperty("stlm_dt")
    private String stlmDt;

    @JsonProperty("idx_cl_code")
    private String idxClCode;

    @JsonProperty("idx_cl_nm")
    private String idxClNm;

    @JsonProperty("idx_code")
    private String idxCode;

    @JsonProperty("idx_nm")
    private String idxNm;

    @JsonProperty("idx_val")
    private String idxVal;
}