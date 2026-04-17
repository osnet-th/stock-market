# ValuationMetricService 예시

DART는 PER/PBR/EPS/BPS 지표를 제공하지 않으므로 재무계정·주식수·현재 주가를 조합해 자체 계산. **최근 1년 단일 값만** 계산한다 (역사 주가 API 부재로 역사 PER/PBR 불가).

## 1. 응답 DTO

```java
// stock/application/dto/ValuationMetricResponse.java
package com.thlee.stock.market.stockmarket.stock.application.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class ValuationMetricResponse {
    private final String termName;             // 기준 회계기 (예: "제55기")
    private final BigDecimal eps;              // nullable
    private final BigDecimal bps;              // nullable
    private final BigDecimal per;              // nullable
    private final BigDecimal pbr;              // nullable
    private final String referencePriceDate;   // PER/PBR 기준 주가 날짜
    private final BigDecimal referencePrice;   // nullable
    private final List<String> warnings;       // 실패·근사 사유
}
```

## 2. 서비스 스켈레톤

```java
// stock/application/ValuationMetricService.java
package com.thlee.stock.market.stockmarket.stock.application;

import com.thlee.stock.market.stockmarket.stock.application.dto.*;
import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValuationMetricService {

    private static final String NET_INCOME_ACCOUNT = "당기순이익";
    private static final String EQUITY_ACCOUNT = "자본총계";
    private static final String REPORT_CODE_ANNUAL = "11011";

    private final StockFinancialService stockFinancialService;
    private final StockPriceService stockPriceService;

    public ValuationMetricResponse calculate(String stockCode) {
        List<String> warnings = new ArrayList<>();
        String year = String.valueOf(LocalDate.now().getYear());

        // 1) 재무계정 (최근 연도 당기순이익·자본총계)
        List<FinancialAccountResponse> accounts = stockFinancialService
                .getFinancialAccounts(stockCode, year, REPORT_CODE_ANNUAL);

        String termName = accounts.stream()
                .map(FinancialAccountResponse::getCurrentTermName)
                .findFirst().orElse(null);

        BigDecimal netIncome = findLatestAmount(accounts, NET_INCOME_ACCOUNT, warnings);
        BigDecimal equity    = findLatestAmount(accounts, EQUITY_ACCOUNT, warnings);

        // 2) 유통주식수
        BigDecimal shares = fetchDistributedShares(stockCode, year, warnings);

        // 3) EPS, BPS
        BigDecimal eps = (netIncome != null && shares != null && shares.signum() != 0)
                ? netIncome.divide(shares, 2, RoundingMode.HALF_UP) : null;
        BigDecimal bps = (equity != null && shares != null && shares.signum() != 0)
                ? equity.divide(shares, 2, RoundingMode.HALF_UP) : null;

        // 4) PER, PBR
        BigDecimal price = safeFetchPrice(stockCode, warnings);
        BigDecimal per = (price != null && eps != null && eps.signum() != 0)
                ? price.divide(eps, 2, RoundingMode.HALF_UP) : null;
        BigDecimal pbr = (price != null && bps != null && bps.signum() != 0)
                ? price.divide(bps, 2, RoundingMode.HALF_UP) : null;

        return new ValuationMetricResponse(
                termName, eps, bps, per, pbr,
                LocalDate.now().toString(), price, warnings
        );
    }

    // 헬퍼:
    // - findLatestAmount(accounts, accountName, warnings) — accountName 일치 행의 currentTermAmount 파싱
    // - fetchDistributedShares(stockCode, year, warnings) — getStockQuantities에서 distributedStockCount 파싱
    // - safeFetchPrice(stockCode, warnings) — StockPriceService.getPrice(KRX)
    // 각 헬퍼는 실패 시 null 반환 + warnings 추가
}
```

## 3. 설계 포인트

- **Application 계층 순수 조합 서비스**. 외부 API 직접 호출 없음.
- **최근 1년만 계산** — 3개년 EPS/BPS에서 주식수 변동에 따른 근사 오차 문제 회피.
- **실패 내성**: 개별 항목이 비어도 다른 값은 반환. warnings 메시지로 LLM 프롬프트에 계산 한계 전달.
- **FINANCIAL 모드 한국주식 한정**: `MarketType.DOMESTIC, ExchangeCode.KRX` 고정. 미국주식 지원은 후속 과제.
- **테스트 가능성**: `StockFinancialService`·`StockPriceService`는 주입형 → Mockito로 대체해 결과값 검증 가능.

## 4. 프롬프트 주입 예시

```
### 가치평가 지표 (기준 회계기: 제55기, 기준 주가일: 2026-04-17)
- EPS: 2,610원
- BPS: 55,800원
- PER: 26.4배
- PBR: 1.23배
- 참고: 유통주식수 조회 성공. 역사 주가 API 부재로 과거 PER/PBR 제공 불가.
```
