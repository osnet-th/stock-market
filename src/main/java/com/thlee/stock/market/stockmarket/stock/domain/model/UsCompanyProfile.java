package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * SEC submissions 기반 기업 프로필. SEC이 제공하지 않는 항목(CEO명·설립일 등)은 필드 자체를 두지 않는다.
 *
 * @param fiscalYearEnd 결산기말 MMDD (예: "0926")
 */
public record UsCompanyProfile(
        String name,
        String sicCode,
        String sicDescription,
        String fiscalYearEnd,
        String exchange,
        String ticker,
        String address,
        String website
) {}
