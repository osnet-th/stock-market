package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DartDisclosureItem {

    @JsonProperty("corp_cls")
    private String corpCls;

    @JsonProperty("corp_name")
    private String corpName;

    @JsonProperty("corp_code")
    private String corpCode;

    @JsonProperty("stock_code")
    private String stockCode;

    @JsonProperty("report_nm")
    private String reportNm;

    @JsonProperty("rcept_no")
    private String rceptNo;

    @JsonProperty("flr_nm")
    private String flrNm;

    @JsonProperty("rcept_dt")
    private String rceptDt;

    @JsonProperty("rm")
    private String rm;
}
