# ReportCode, IndexClassCode enum 예시

## ReportCode

위치: `stock/domain/model/ReportCode.java`

```java
package com.thlee.stock.market.stockmarket.stock.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * DART 보고서 구분 코드
 */
@Getter
@RequiredArgsConstructor
public enum ReportCode {

    ANNUAL("11011", "사업보고서"),
    SEMI_ANNUAL("11012", "반기보고서"),
    Q1("11013", "1분기보고서"),
    Q3("11014", "3분기보고서");

    private final String code;
    private final String label;
}
```

## IndexClassCode

위치: `stock/domain/model/IndexClassCode.java`

```java
package com.thlee.stock.market.stockmarket.stock.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * DART 재무지표 분류 코드
 */
@Getter
@RequiredArgsConstructor
public enum IndexClassCode {

    PROFITABILITY("M210000", "수익성지표"),
    STABILITY("M220000", "안정성지표"),
    GROWTH("M230000", "성장성지표"),
    ACTIVITY("M240000", "활동성지표");

    private final String code;
    private final String label;
}
```