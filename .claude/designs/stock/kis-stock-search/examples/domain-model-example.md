# 도메인 모델 구현 예시

## MarketType

```java
package com.thlee.stock.market.stockmarket.stock.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 주식 시장 구분
 */
@Getter
@RequiredArgsConstructor
public enum MarketType {

    // 국내
    KOSPI("kospi_code", true),
    KOSDAQ("kosdaq_code", true),
    KONEX("konex_code", true),

    // 해외 - 미국
    NASDAQ("nas", false),
    NYSE("nys", false),
    AMEX("ams", false),

    // 해외 - 중국
    SHANGHAI("shs", false),
    SHANGHAI_INDEX("shi", false),
    SHENZHEN("szs", false),
    SHENZHEN_INDEX("szi", false),

    // 해외 - 일본
    TOKYO("tse", false),

    // 해외 - 홍콩
    HONGKONG("hks", false),

    // 해외 - 베트남
    HANOI("hnx", false),
    HOCHIMINH("hsx", false);

    private final String masterFileCode; // 마스터파일 다운로드 시 사용하는 코드
    private final boolean domestic;
}
```

## ExchangeCode

```java
package com.thlee.stock.market.stockmarket.stock.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 거래소 코드
 */
@Getter
@RequiredArgsConstructor
public enum ExchangeCode {

    // 국내
    KRX("한국거래소"),

    // 미국
    NAS("나스닥"),
    NYS("뉴욕증권거래소"),
    AMS("아멕스"),

    // 중국
    SHS("상해증권거래소"),
    SHI("상해지수"),
    SZS("심천증권거래소"),
    SZI("심천지수"),

    // 일본
    TSE("도쿄증권거래소"),

    // 홍콩
    HKS("홍콩증권거래소"),

    // 베트남
    HNX("하노이증권거래소"),
    HSX("호치민증권거래소");

    private final String description;
}
```

## Stock

```java
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
```

## StockResponse

```java
package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.Stock;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StockResponse {
    private final String stockCode;
    private final String stockName;
    private final String englishName;
    private final String marketType;
    private final String exchangeCode;

    public static StockResponse from(Stock stock) {
        return new StockResponse(
            stock.stockCode(),
            stock.stockName(),
            stock.englishName(),
            stock.marketType().name(),
            stock.exchangeCode().name()
        );
    }
}
```