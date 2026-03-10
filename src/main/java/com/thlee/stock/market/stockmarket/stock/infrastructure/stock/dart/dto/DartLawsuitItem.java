package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DartLawsuitItem {

    @JsonProperty("rcept_no")
    private String rceptNo;

    @JsonProperty("corp_cls")
    private String corpCls;

    @JsonProperty("corp_code")
    private String corpCode;

    @JsonProperty("corp_name")
    private String corpName;

    @JsonProperty("icnm")
    private String icnm;

    @JsonProperty("ac_ap")
    private String acAp;

    @JsonProperty("rq_cn")
    private String rqCn;

    @JsonProperty("cpct")
    private String cpct;

    @JsonProperty("ft_ctp")
    private String ftCtp;

    @JsonProperty("lgd")
    private String lgd;

    @JsonProperty("cfd")
    private String cfd;
}