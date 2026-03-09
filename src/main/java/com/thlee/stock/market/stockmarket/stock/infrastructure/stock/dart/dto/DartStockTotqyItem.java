package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DartStockTotqyItem {

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

    @JsonProperty("isu_stock_totqy")
    private String isuStockTotqy;

    @JsonProperty("now_to_isu_stock_totqy")
    private String nowToIsuStockTotqy;

    @JsonProperty("now_to_dcrs_stock_totqy")
    private String nowToDcrsStockTotqy;

    @JsonProperty("redc")
    private String redc;

    @JsonProperty("profit_incnr")
    private String profitIncnr;

    @JsonProperty("rdmstk_repy")
    private String rdmstkRepy;

    @JsonProperty("etc")
    private String etc;

    @JsonProperty("istc_totqy")
    private String istcTotqy;

    @JsonProperty("tesstk_co")
    private String tesstkCo;

    @JsonProperty("distb_stock_co")
    private String distbStockCo;

    @JsonProperty("stlm_dt")
    private String stlmDt;
}