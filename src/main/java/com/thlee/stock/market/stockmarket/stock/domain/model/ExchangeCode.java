package com.thlee.stock.market.stockmarket.stock.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 거래소 코드
 */
@Getter
@RequiredArgsConstructor
public enum ExchangeCode {

    // 국내
    KRX("한국거래소", "KRW"),

    // 미국
    NAS("나스닥", "USD"),
    NYS("뉴욕증권거래소", "USD"),
    AMS("아멕스", "USD"),

    // 중국
    SHS("상해증권거래소", "CNY"),
    SHI("상해지수", "CNY"),
    SZS("심천증권거래소", "CNY"),
    SZI("심천지수", "CNY"),

    // 일본
    TSE("도쿄증권거래소", "JPY"),

    // 홍콩
    HKS("홍콩증권거래소", "HKD"),

    // 베트남
    HNX("하노이증권거래소", "VND"),
    HSX("호치민증권거래소", "VND");

    private final String description;
    private final String currency;
}