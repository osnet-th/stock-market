package com.thlee.stock.market.stockmarket.stocknote.domain.model;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.ImpactLevel;

/**
 * 기록 작성 시점의 펀더멘털 영향도 묶음 (StockNote 의 1:1 흡수 그룹).
 * 모든 ImpactLevel 필드 nullable — 사용자가 일부만 입력 가능. boolean 은 default false.
 */
public record FundamentalImpact(
        ImpactLevel revenueImpact,
        ImpactLevel profitImpact,
        ImpactLevel cashflowImpact,
        boolean oneTime,
        boolean structural
) {
    public static FundamentalImpact empty() {
        return new FundamentalImpact(null, null, null, false, false);
    }
}
