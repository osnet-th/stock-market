package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DartAlotMatterItem {

    @JsonProperty("rcept_no")
    private String rceptNo;

    @JsonProperty("corp_cls")
    private String corpCls;

    @JsonProperty("corp_code")
    private String corpCode;

    @JsonProperty("corp_name")
    private String corpName;

    @JsonProperty("se")
    private String se;

    @JsonProperty("stock_knd")
    private String stockKnd;

    @JsonProperty("thstrm")
    private String thstrm;

    @JsonProperty("frmtrm")
    private String frmtrm;

    @JsonProperty("lwfr")
    private String lwfr;

    @JsonProperty("stlm_dt")
    private String stlmDt;
}