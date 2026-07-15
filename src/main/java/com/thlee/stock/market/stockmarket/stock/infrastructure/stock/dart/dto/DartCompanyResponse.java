package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기업개황(company.json) 응답.
 * {@link DartApiResponse}와 달리 list 없이 단일 객체 필드가 최상위에 온다.
 */
@Getter
@NoArgsConstructor
public class DartCompanyResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("corp_code")
    private String corpCode;

    @JsonProperty("corp_name")
    private String corpName;

    @JsonProperty("corp_name_eng")
    private String corpNameEng;

    @JsonProperty("stock_name")
    private String stockName;

    @JsonProperty("stock_code")
    private String stockCode;

    @JsonProperty("ceo_nm")
    private String ceoNm;

    @JsonProperty("corp_cls")
    private String corpCls;

    @JsonProperty("jurir_no")
    private String jurirNo;

    @JsonProperty("bizr_no")
    private String bizrNo;

    @JsonProperty("adres")
    private String adres;

    @JsonProperty("hm_url")
    private String hmUrl;

    @JsonProperty("ir_url")
    private String irUrl;

    @JsonProperty("phn_no")
    private String phnNo;

    @JsonProperty("fax_no")
    private String faxNo;

    @JsonProperty("induty_code")
    private String indutyCode;

    @JsonProperty("est_dt")
    private String estDt;

    @JsonProperty("acc_mt")
    private String accMt;

    public boolean isSuccess() {
        return getStatusCode().isSuccess();
    }

    public boolean isNoData() {
        return getStatusCode().isNoData();
    }

    public DartStatusCode getStatusCode() {
        return DartStatusCode.fromCode(status);
    }
}
