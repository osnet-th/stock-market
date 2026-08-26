package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 최대주주 현황(hyslrSttus.json) 항목
 */
@Getter
@NoArgsConstructor
public class DartHyslrSttusItem {

    @JsonProperty("rcept_no")
    private String rceptNo;

    @JsonProperty("corp_cls")
    private String corpCls;

    @JsonProperty("corp_code")
    private String corpCode;

    @JsonProperty("corp_name")
    private String corpName;

    @JsonProperty("stock_knd")
    private String stockKnd;

    @JsonProperty("nm")
    private String nm;

    @JsonProperty("relate")
    private String relate;

    @JsonProperty("bsis_posesn_stock_co")
    private String bsisPosesnStockCo;

    @JsonProperty("bsis_posesn_stock_qota_rt")
    private String bsisPosesnStockQotaRt;

    @JsonProperty("trmend_posesn_stock_co")
    private String trmendPosesnStockCo;

    @JsonProperty("trmend_posesn_stock_qota_rt")
    private String trmendPosesnStockQotaRt;

    @JsonProperty("rm")
    private String rm;

    @JsonProperty("stlm_dt")
    private String stlmDt;
}
