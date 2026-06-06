package com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KIS 주식기본조회 API 응답 output (요약 카드용 핵심 필드).
 * tr_id: CTPF1002R. (전체 60+ 필드 중 카드 표시에 쓰는 것만 매핑)
 */
@Getter
@NoArgsConstructor
public class KisSearchStockInfoOutput {

    @JsonProperty("prdt_name")
    private String name;                 // 상품명 (삼성전자보통주)

    @JsonProperty("prdt_abrv_name")
    private String abbrName;             // 상품약어명 (삼성전자)

    @JsonProperty("prdt_eng_abrv_name")
    private String engAbbrName;          // 영문약어명 (SamsungElec)

    @JsonProperty("std_pdno")
    private String stdCode;              // 표준상품번호 (ISIN, KR7005930003)

    @JsonProperty("mket_id_cd")
    private String marketIdCode;         // 시장ID코드 (STK:코스피, KSQ:코스닥)

    @JsonProperty("setl_mmdd")
    private String settlementMonthDay;   // 결산월(일)

    @JsonProperty("lstg_stqt")
    private String listedShares;         // 상장주수

    @JsonProperty("cpta")
    private String capital;              // 자본금

    @JsonProperty("papr")
    private String parValue;             // 액면가

    @JsonProperty("issu_pric")
    private String issuePrice;           // 발행가격

    @JsonProperty("kospi200_item_yn")
    private String kospi200Yn;           // 코스피200 종목 여부

    @JsonProperty("scts_mket_lstg_dt")
    private String kospiListingDate;     // 유가증권시장 상장일자

    @JsonProperty("kosdaq_mket_lstg_dt")
    private String kosdaqListingDate;    // 코스닥시장 상장일자

    @JsonProperty("std_idst_clsf_cd_name")
    private String industryName;         // 표준산업분류 코드명 (업종)

    @JsonProperty("tr_stop_yn")
    private String tradingStopYn;        // 거래정지 여부

    @JsonProperty("admn_item_yn")
    private String managedYn;            // 관리종목 여부

    @JsonProperty("thdt_clpr")
    private String closePrice;           // 당일 종가

    @JsonProperty("bfdy_clpr")
    private String prevClosePrice;       // 전일 종가

    @JsonProperty("frnr_psnl_lmt_rt")
    private String foreignLimitRate;     // 외국인 개인한도 비율
}
