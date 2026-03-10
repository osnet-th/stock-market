package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DartSinglAcntAllItem {

    @JsonProperty("rcept_no")
    private String rceptNo;

    @JsonProperty("reprt_code")
    private String reprtCode;

    @JsonProperty("bsns_year")
    private String bsnsYear;

    @JsonProperty("corp_code")
    private String corpCode;

    @JsonProperty("sj_div")
    private String sjDiv;

    @JsonProperty("sj_nm")
    private String sjNm;

    @JsonProperty("account_id")
    private String accountId;

    @JsonProperty("account_nm")
    private String accountNm;

    @JsonProperty("account_detail")
    private String accountDetail;

    @JsonProperty("thstrm_nm")
    private String thstrmNm;

    @JsonProperty("thstrm_amount")
    private String thstrmAmount;

    @JsonProperty("frmtrm_nm")
    private String frmtrmNm;

    @JsonProperty("frmtrm_amount")
    private String frmtrmAmount;

    @JsonProperty("ord")
    private String ord;

    @JsonProperty("currency")
    private String currency;
}