# 도메인 모델 예시

## StockPrice (도메인 모델)

```java
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
```

## StockPricePort (도메인 포트 인터페이스)

```java
package com.thlee.stock.market.stockmarket.stock.domain.service;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.domain.model.StockPrice;

/**
 * 주식 현재가 조회 포트
 */
public interface StockPricePort {
    StockPrice getPrice(String stockCode, MarketType marketType, ExchangeCode exchangeCode);
}
```

## ExchangeCode 매핑 메서드 추가

기존 `ExchangeCode` enum에 KIS 해외 API 요청 시 사용하는 거래소코드(`EXCD`) 매핑을 추가한다.

```java
// ExchangeCode enum에 추가할 필드 및 메서드

@Getter
@RequiredArgsConstructor
public enum ExchangeCode {

    // 국내
    KRX("한국거래소", ""),

    // 미국
    NAS("나스닥", "NAS"),
    NYS("뉴욕증권거래소", "NYS"),
    AMS("아멕스", "AMS"),

    // 중국
    SHS("상해증권거래소", "SHS"),
    SHI("상해지수", "SHI"),
    SZS("심천증권거래소", "SZS"),
    SZI("심천지수", "SZI"),

    // 일본
    TSE("도쿄증권거래소", "TSE"),

    // 홍콩
    HKS("홍콩증권거래소", "HKS"),

    // 베트남
    HNX("하노이증권거래소", "HNX"),
    HSX("호치민증권거래소", "HSX");

    private final String description;
    private final String kisCode;  // KIS 해외 API EXCD 파라미터용 코드
}
```

> **참고**: KIS 해외 API의 `EXCD` 파라미터 값이 `ExchangeCode` enum 이름과 동일하므로, `kisCode` 대신 `name()`을 사용할 수도 있다. 단, KRX는 국내 전용이므로 해외 API에서는 사용하지 않는다.
> 실제 구현 시 `name()`을 직접 사용하고, `kisCode` 필드 추가 없이 처리 가능하다면 기존 enum을 수정하지 않아도 된다.