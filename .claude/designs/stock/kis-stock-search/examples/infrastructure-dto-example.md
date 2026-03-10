# KisMasterStock 구현 예시

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * KIS 마스터파일 파싱 결과 (국내/해외 공통)
 */
@Getter
@RequiredArgsConstructor
public class KisMasterStock {
    private final String stockCode;          // 종목코드 (국내: 005930, 해외: AAPL)
    private final String koreanName;         // 한글 종목명
    private final String englishName;        // 영문 종목명 (국내는 null)
    private final MarketType marketType;     // 시장구분
    private final ExchangeCode exchangeCode; // 거래소코드
}
```