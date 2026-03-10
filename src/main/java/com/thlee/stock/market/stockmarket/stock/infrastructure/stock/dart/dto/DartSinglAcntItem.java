package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DartSinglAcntItem {

    @JsonProperty("rcept_no")
    private String rceptNo;

    @JsonProperty("bsns_year")
    private String bsnsYear;

    @JsonProperty("stock_code")
    private String stockCode;

    @JsonProperty("reprt_code")
    private String reprtCode;

    @JsonProperty("account_nm")
    private String accountNm;

    @JsonProperty("fs_div")
    private String fsDiv;

    @JsonProperty("fs_nm")
    private String fsNm;

    @JsonProperty("sj_div")
    private String sjDiv;

    @JsonProperty("sj_nm")
    private String sjNm;

    @JsonProperty("thstrm_nm")
    private String thstrmNm;

    @JsonProperty("thstrm_amount")
    private String thstrmAmount;

    @JsonProperty("frmtrm_nm")
    private String frmtrmNm;

    @JsonProperty("frmtrm_amount")
    private String frmtrmAmount;

    @JsonProperty("bfefrmtrm_nm")
    private String bfefrmtrmNm;

    @JsonProperty("bfefrmtrm_amount")
    private String bfefrmtrmAmount;

    @JsonProperty("ord")
    private String ord;

    @JsonProperty("currency")
    private String currency;
}