package com.thlee.stock.market.stockmarket.economics.domain.model;

import static com.thlee.stock.market.stockmarket.economics.domain.model.IndicatorCategory.*;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GlobalEconomicIndicatorType {
    // 무역/GDP
    BALANCE_OF_TRADE(TRADE_GDP, "balance-of-trade", "무역수지"),
    GDP_ANNUAL_GROWTH_RATE(TRADE_GDP, "gdp-annual-growth-rate", "GDP 성장률"),
    GDP(TRADE_GDP, "gdp", "GDP (총생산)"),
    GDP_FROM_AGRICULTURE(TRADE_GDP, "gdp-from-agriculture", "GDP 농업"),
    GDP_FROM_CONSTRUCTION(TRADE_GDP, "gdp-from-construction", "GDP 건설업"),
    GDP_FROM_MANUFACTURING(TRADE_GDP, "gdp-from-manufacturing", "GDP 제조업"),
    GDP_FROM_MINING(TRADE_GDP, "gdp-from-mining", "GDP 광업"),
    GDP_FROM_PUBLIC_ADMINISTRATION(TRADE_GDP, "gdp-from-public-administration", "GDP 공공행정"),
    GDP_FROM_SERVICES(TRADE_GDP, "gdp-from-services", "GDP 서비스업"),
    GDP_FROM_TRANSPORT(TRADE_GDP, "gdp-from-transport", "GDP 운송업"),
    GDP_FROM_UTILITIES(TRADE_GDP, "gdp-from-utilities", "GDP 유틸리티"),

    // 고용
    UNEMPLOYMENT_RATE(EMPLOYMENT, "unemployment-rate", "실업률"),
    WAGE_GROWTH(EMPLOYMENT, "wage-growth", "임금 상승률"),
    EMPLOYMENT_CHANGE(EMPLOYMENT, "employment-change", "고용 변화"),

    // 물가/소비
    PRODUCER_PRICES(PRICE_CONSUMPTION, "producer-prices", "생산자 물가 지수"),
    CONSUMER_PRICE_INDEX(PRICE_CONSUMPTION, "consumer-price-index-cpi", "소비자 물가 지수"),
    CORE_CONSUMER_PRICES(PRICE_CONSUMPTION, "core-consumer-prices", "핵심 소비자 물가 지수"),
    RETAIL_SALES_MOM(PRICE_CONSUMPTION, "retail-sales-mom", "월간 소매 판매"),
    INFLATION_RATE(PRICE_CONSUMPTION, "inflation-rate", "물가상승률"),
    INFLATION_EXPECTATIONS(PRICE_CONSUMPTION, "inflation-expectations", "인플레이션 기대 예상치"),
    INFLATION_RATE_MOM(PRICE_CONSUMPTION, "inflation-rate-mom", "인플레이션(월간)"),

    // 금리/금융
    INTEREST_RATE(RATE_FINANCE, "interest-rate", "기준 금리"),
    CENTRAL_BANK_BALANCE_SHEET(RATE_FINANCE, "central-bank-balance-sheet", "중앙 은행 대차 대조표"),
    GOLD_RESERVES(RATE_FINANCE, "gold-reserves", "황금 보유"),
    LENDING_RATE(RATE_FINANCE, "lending-rate", "대출금리"),
    INTERBANK_RATE(RATE_FINANCE, "interbank-rate", "은행간 금리"),

    // 신뢰/선행 지수
    LEADING_ECONOMIC_INDEX(CONFIDENCE_LEADING, "leading-economic-index", "경기선행지수"),
    CONSUMER_CONFIDENCE(CONFIDENCE_LEADING, "consumer-confidence", "소비자 신뢰 지수"),
    BUSINESS_CONFIDENCE(CONFIDENCE_LEADING, "business-confidence", "기업 신뢰 지수"),

    // PMI/산업
    MANUFACTURING_PMI(PMI_INDUSTRY, "manufacturing-pmi", "제조업 PMI"),
    SERVICES_PMI(PMI_INDUSTRY, "services-pmi", "서비스업 PMI"),
    INDUSTRIAL_PRODUCTION_MOM(PMI_INDUSTRY, "industrial-production-mom", "산업 생산"),

    // 재정/국가
    GOVERNMENT_DEBT(FISCAL_STATE, "government-debt", "정부 부채"),
    GOVERNMENT_REVENUES(FISCAL_STATE, "government-revenues", "재정 수입"),
    FISCAL_EXPENDITURE(FISCAL_STATE, "fiscal-expenditure", "재정 지출"),
    CURRENT_ACCOUNT(FISCAL_STATE, "current-account", "경상 수지"),
    FOREIGN_EXCHANGE_RESERVES(FISCAL_STATE, "foreign-exchange-reserves", "외환 보유고"),

    // 통화량
    MONEY_SUPPLY_M0(MONEY_SUPPLY, "money-supply-m0", "1개월 미만 통화 공급량"),
    MONEY_SUPPLY_M1(MONEY_SUPPLY, "money-supply-m1", "1개월 통화공급량"),
    MONEY_SUPPLY_M2(MONEY_SUPPLY, "money-supply-m2", "2개월 통화 공급량"),
    MONEY_SUPPLY_M3(MONEY_SUPPLY, "money-supply-m3", "3개월 통화공급량");

    private final IndicatorCategory category;
    private final String pathSegment;
    private final String displayName;
}