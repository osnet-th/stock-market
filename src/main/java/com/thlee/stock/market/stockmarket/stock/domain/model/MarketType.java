package com.thlee.stock.market.stockmarket.stock.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 주식 시장 구분
 */
@Getter
@RequiredArgsConstructor
public enum MarketType {

    // 국내
    KOSPI("kospi_code", true),
    KOSDAQ("kosdaq_code", true),
    KONEX("konex_code", true),

    // 해외 - 미국
    NASDAQ("nas", false),
    NYSE("nys", false),
    AMEX("ams", false),

    // 해외 - 중국
    SHANGHAI("shs", false),
    SHANGHAI_INDEX("shi", false),
    SHENZHEN("szs", false),
    SHENZHEN_INDEX("szi", false),

    // 해외 - 일본
    TOKYO("tse", false),

    // 해외 - 홍콩
    HONGKONG("hks", false),

    // 해외 - 베트남
    HANOI("hnx", false),
    HOCHIMINH("hsx", false);

    private final String masterFileCode; // 마스터파일 다운로드 시 사용하는 코드
    private final boolean domestic;
}