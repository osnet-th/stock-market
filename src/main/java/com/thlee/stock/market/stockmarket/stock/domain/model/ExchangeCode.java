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
    KRX("한국거래소"),

    // 미국
    NAS("나스닥"),
    NYS("뉴욕증권거래소"),
    AMS("아멕스"),

    // 중국
    SHS("상해증권거래소"),
    SHI("상해지수"),
    SZS("심천증권거래소"),
    SZI("심천지수"),

    // 일본
    TSE("도쿄증권거래소"),

    // 홍콩
    HKS("홍콩증권거래소"),

    // 베트남
    HNX("하노이증권거래소"),
    HSX("호치민증권거래소");

    private final String description;
}