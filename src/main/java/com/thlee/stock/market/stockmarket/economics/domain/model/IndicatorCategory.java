package com.thlee.stock.market.stockmarket.economics.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IndicatorCategory {
    TRADE_GDP("무역/GDP"),
    EMPLOYMENT("고용"),
    PRICE_CONSUMPTION("물가/소비"),
    RATE_FINANCE("금리/금융"),
    CONFIDENCE_LEADING("신뢰/선행 지수"),
    PMI_INDUSTRY("PMI/산업"),
    FISCAL_STATE("재정/국가"),
    MONEY_SUPPLY("통화량");

    private final String displayName;
}