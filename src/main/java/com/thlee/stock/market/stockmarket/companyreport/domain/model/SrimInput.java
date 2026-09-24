package com.thlee.stock.market.stockmarket.companyreport.domain.model;

import java.math.BigDecimal;
import java.util.List;

/** 비율은 소수, 금액은 리포트 기본 통화 단위. 날짜는 ISO 일자 문자열. */
public record SrimInput(BigDecimal equity, String equityDate, BigDecimal shares, String sharesDate,
                        BigDecimal requiredReturn, String currency, BigDecimal referencePrice,
                        String referencePriceDate, List<Year> years) {
    public SrimInput {
        equity = normalized(equity);
        shares = normalized(shares);
        requiredReturn = normalized(requiredReturn);
        referencePrice = normalized(referencePrice);
        years = years == null ? List.of() : List.copyOf(years);
    }

    public record Year(Integer year, String mode, BigDecimal directRoe,
                       BigDecimal previousEquity, BigDecimal expectedEquity, BigDecimal expectedIncome) {
        public Year {
            directRoe = normalized(directRoe);
            previousEquity = normalized(previousEquity);
            expectedEquity = normalized(expectedEquity);
            expectedIncome = normalized(expectedIncome);
        }
    }

    private static BigDecimal normalized(BigDecimal value) {
        if (value == null) return null;
        BigDecimal stripped = value.stripTrailingZeros();
        // Do not expand malicious exponents before numeric validation.
        if (stripped.scale() < -24) throw new IllegalArgumentException("숫자의 자릿수가 허용 범위를 초과합니다.");
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
