package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대량보유 상황보고(majorstock.json) 항목
 */
@Getter
@NoArgsConstructor
public class DartMajorstockItem {

    @JsonProperty("rcept_no")
    private String rceptNo;

    @JsonProperty("rcept_dt")
    private String rceptDt;

    @JsonProperty("corp_code")
    private String corpCode;

    @JsonProperty("corp_name")
    private String corpName;

    @JsonProperty("report_tp")
    private String reportTp;

    @JsonProperty("repror")
    private String repror;

    @JsonProperty("stkqy")
    private String stkqy;

    @JsonProperty("stkqy_irds")
    private String stkqyIrds;

    @JsonProperty("stkrt")
    private String stkrt;

    @JsonProperty("stkrt_irds")
    private String stkrtIrds;

    @JsonProperty("ctr_stkqy")
    private String ctrStkqy;

    @JsonProperty("ctr_stkrt")
    private String ctrStkrt;

    @JsonProperty("report_resn")
    private String reportResn;
}
