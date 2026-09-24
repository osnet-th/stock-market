package com.thlee.stock.market.stockmarket.companyreport.domain.model;

/**
 * 종목 코드의 시장/통화 판별. 6자리 숫자는 국내(KRW), 그 외는 미국(USD)으로 본다.
 * 동일 규칙이 여러 곳에 흩어지지 않도록 이 클래스를 단일 출처로 사용한다.
 */
public final class StockMarketCode {

    private static final String CURRENCY_KRW = "KRW";
    private static final String CURRENCY_USD = "USD";

    private StockMarketCode() {
    }

    public static boolean isDomestic(String stockCode) {
        return stockCode != null && stockCode.matches("\\d{6}");
    }

    public static String currencyOf(String stockCode) {
        return isDomestic(stockCode) ? CURRENCY_KRW : CURRENCY_USD;
    }
}
