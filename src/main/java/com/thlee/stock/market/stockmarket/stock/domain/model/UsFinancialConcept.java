package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * 미국 리포트 조달용 재무 개념 어휘.
 * us-gaap/dei 태그 매핑(폴백 체인 포함)은 인프라 어댑터가 담당하고, 도메인·리포트 계층은 이 개념만 참조한다.
 */
public enum UsFinancialConcept {

    // 손익
    REVENUE,
    COST_OF_REVENUE,
    SGA_EXPENSE,
    OPERATING_INCOME,
    PRETAX_INCOME,
    NET_INCOME,
    /** 지배주주 귀속 순이익 전용 (폴백 없음) — ROE 지배주주 기준 판정용 */
    NET_INCOME_TO_PARENT,

    // 현금흐름
    OPERATING_CF,
    INVESTING_CF,
    FINANCING_CF,
    CAPEX,
    DEPRECIATION_AMORTIZATION,

    // 재무상태
    TOTAL_ASSETS,
    CURRENT_ASSETS,
    TOTAL_LIABILITIES,
    CURRENT_LIABILITIES,
    EQUITY,
    /** 지배주주 귀속 자본 전용 (폴백 없음) — ROE 지배주주 기준 판정용 */
    EQUITY_OF_PARENT,
    RETAINED_EARNINGS,
    CAPITAL_STOCK,

    // 가치평가 입력 (자산 세부)
    CASH,
    SECURITIES_CURRENT,
    RECEIVABLES,
    INVENTORY,
    INVESTMENTS_NONCURRENT,
    TANGIBLE_ASSETS,
    GOODWILL,
    INTANGIBLE_ASSETS,

    // 차입금
    DEBT_NONCURRENT,
    DEBT_CURRENT,
    SHORT_TERM_BORROWINGS,

    // 주당/주식수 (EPS·DPS는 USD/shares, 주식수는 shares 단위)
    EPS_DILUTED,
    EPS_BASIC,
    DPS_DECLARED,
    DIVIDENDS_PAID,
    SHARES_OUTSTANDING
}
