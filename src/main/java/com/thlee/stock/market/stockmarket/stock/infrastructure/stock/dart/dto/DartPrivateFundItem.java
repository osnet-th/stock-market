package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사모자금 사용내역 응답 DTO
 * - 2018.01.18 이전: payAmount, cptalUsePlan, realCptalUseSttus
 * - 2018.01.19 이후: mtrptCptalUsePlan*, realCptalUseDtls*
 */
@Getter
@NoArgsConstructor
public class DartPrivateFundItem {

    @JsonProperty("rcept_no")
    private String rceptNo;

    @JsonProperty("corp_cls")
    private String corpCls;

    @JsonProperty("corp_code")
    private String corpCode;

    @JsonProperty("corp_name")
    private String corpName;

    @JsonProperty("se_nm")
    private String seNm;

    @JsonProperty("tm")
    private String tm;

    @JsonProperty("pay_de")
    private String payDe;

    // 2018.01.18 이전 필드
    @JsonProperty("pay_amount")
    private String payAmount;

    @JsonProperty("cptal_use_plan")
    private String cptalUsePlan;

    @JsonProperty("real_cptal_use_sttus")
    private String realCptalUseSttus;

    // 2018.01.19 이후 필드
    @JsonProperty("mtrpt_cptal_use_plan_useprps")
    private String mtrptCptalUsePlanUseprps;

    @JsonProperty("mtrpt_cptal_use_plan_prcure_amount")
    private String mtrptCptalUsePlanPrcureAmount;

    @JsonProperty("real_cptal_use_dtls_cn")
    private String realCptalUseDtlsCn;

    @JsonProperty("real_cptal_use_dtls_amount")
    private String realCptalUseDtlsAmount;

    @JsonProperty("dffrnc_occrrnc_resn")
    private String dffrncOccrrncResn;

    @JsonProperty("stlm_dt")
    private String stlmDt;
}