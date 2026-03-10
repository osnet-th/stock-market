package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * 상장 종목 정보 (국내/해외 공통 도메인 모델)
 */
public record Stock(
    String stockCode,          // 종목코드 (국내: 005930, 해외: AAPL)
    String stockName,          // 한글 종목명
    String englishName,        // 영문 종목명 (국내는 null)
    MarketType marketType,     // 시장구분
    ExchangeCode exchangeCode  // 거래소코드
) {
}