package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * 주식 현재가 정보 (국내/해외 공통 도메인 모델)
 */
public record StockPrice(
    String stockCode,          // 종목코드 (국내: 005930, 해외: AAPL)
    String currentPrice,       // 현재가
    String previousClose,      // 전일 종가
    String change,             // 전일 대비
    String changeSign,         // 대비 부호 (1=상한, 2=상승, 3=보합, 4=하한, 5=하락)
    String changeRate,         // 전일 대비율 (%)
    String volume,             // 누적 거래량
    String tradingAmount,      // 누적 거래대금
    String high,               // 고가 (국내만, 해외는 null)
    String low,                // 저가 (국내만, 해외는 null)
    String open,               // 시가 (국내만, 해외는 null)
    MarketType marketType,
    ExchangeCode exchangeCode
) {
}