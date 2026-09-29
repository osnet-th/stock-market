package com.thlee.stock.market.stockmarket.economics.domain.model;

import java.util.List;

/**
 * 기준수익률 채권 종류. 출처와 무관한 조회 대상 분류다.
 */
public enum BondYieldType {
    TREASURY,
    CORPORATE_PUBLIC_UNSECURED;

    /** 국고채 조회 대상 만기(개월): 1·3·5·10·20·30년. 회사채는 출처가 제공하는 만기를 그대로 쓴다. */
    public static final List<Integer> TREASURY_MATURITY_MONTHS = List.of(12, 36, 60, 120, 240, 360);

    /** 신용등급으로 구분하는 종류인지. 회사채만 등급이 있다. */
    public boolean isGraded() {
        return this == CORPORATE_PUBLIC_UNSECURED;
    }
}
