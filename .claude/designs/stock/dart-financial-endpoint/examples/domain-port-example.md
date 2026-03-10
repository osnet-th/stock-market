# 도메인 모델 + 포트 구현 예시

## FinancialAccount (재무계정 - 단일/다중 공용)

```java
package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * DART 재무계정 도메인 모델.
 * 단일/다중회사 재무계정 공용. 다중 전용 필드(thstrmDt, thstrmAddAmount 등)는 단일 조회 시 null.
 */
public record FinancialAccount(
    String rceptNo,
    String bsnsYear,
    String stockCode,
    String reprtCode,
    String accountNm,
    String fsDiv,
    String fsNm,
    String sjDiv,
    String sjNm,
    String thstrmNm,
    String thstrmDt,            // 다중 전용 (단일 시 null)
    String thstrmAmount,
    String thstrmAddAmount,     // 다중 전용 (단일 시 null)
    String frmtrmNm,
    String frmtrmDt,            // 다중 전용 (단일 시 null)
    String frmtrmAmount,
    String frmtrmAddAmount,     // 다중 전용 (단일 시 null)
    String bfefrmtrmNm,
    String bfefrmtrmDt,         // 다중 전용 (단일 시 null)
    String bfefrmtrmAmount,
    String ord,
    String currency
) {}
```

## FinancialIndex (재무지표 - 단일/다중 공용)

```java
package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * DART 재무지표 도메인 모델.
 * 단일/다중회사 재무지표 공용 (동일 구조).
 */
public record FinancialIndex(
    String reprtCode,
    String bsnsYear,
    String corpCode,
    String stockCode,
    String stlmDt,
    String idxClCode,
    String idxClNm,
    String idxCode,
    String idxNm,
    String idxVal
) {}
```

## FullFinancialStatement (전체 재무제표)

```java
package com.thlee.stock.market.stockmarket.stock.domain.model;

public record FullFinancialStatement(
    String rceptNo,
    String reprtCode,
    String bsnsYear,
    String corpCode,
    String sjDiv,
    String sjNm,
    String accountId,
    String accountNm,
    String accountDetail,
    String thstrmNm,
    String thstrmAmount,
    String frmtrmNm,
    String frmtrmAmount,
    String ord,
    String currency
) {}
```

## StockTotalQuantity (주식 총수 현황)

```java
package com.thlee.stock.market.stockmarket.stock.domain.model;

public record StockTotalQuantity(
    String rceptNo,
    String corpCls,
    String corpCode,
    String corpName,
    String se,
    String isuStockTotqy,
    String nowToIsuStockTotqy,
    String nowToDcrsStockTotqy,
    String redc,
    String profitIncnr,
    String rdmstkRepy,
    String etc,
    String istcTotqy,
    String tesstkCo,
    String distbStockCo,
    String stlmDt
) {}
```

## DividendInfo (배당)

```java
package com.thlee.stock.market.stockmarket.stock.domain.model;

public record DividendInfo(
    String rceptNo,
    String corpCls,
    String corpCode,
    String corpName,
    String se,
    String stockKnd,
    String thstrm,
    String frmtrm,
    String lwfr,
    String stlmDt
) {}
```

## LawsuitInfo (소송)

```java
package com.thlee.stock.market.stockmarket.stock.domain.model;

public record LawsuitInfo(
    String rceptNo,
    String corpCls,
    String corpCode,
    String corpName,
    String icnm,
    String acAp,
    String rqCn,
    String cpct,
    String ftCtp,
    String lgd,
    String cfd
) {}
```

## FundUsage (자금사용내역 - 사모/공모 공용)

```java
package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * DART 자금사용내역 도메인 모델.
 * 사모/공모 공용. 구/신 버전 필드를 정규화하여 통합.
 *
 * 정규화 매핑:
 * - capitalUsePlan: cptal_use_plan (사모) / on_dclrt_cptal_use_plan (공모)
 * - planPurpose: mtrpt_cptal_use_plan_useprps (사모) / rs_cptal_use_plan_useprps (공모)
 * - planAmount: mtrpt_cptal_use_plan_prcure_amount (사모) / rs_cptal_use_plan_prcure_amount (공모)
 */
public record FundUsage(
    String rceptNo,
    String corpCls,
    String corpCode,
    String corpName,
    String seNm,
    String tm,
    String payDe,
    // 구버전 필드 (2018.01.18 이전)
    String payAmount,
    String capitalUsePlan,
    String actualCapitalUsage,
    // 신버전 필드 (2018.01.19 이후)
    String planPurpose,
    String planAmount,
    String actualDetailContent,
    String actualDetailAmount,
    String differenceReason,
    String stlmDt
) {}
```

## DartFinancialPort (포트 인터페이스)

```java
package com.thlee.stock.market.stockmarket.stock.domain.service;

import com.thlee.stock.market.stockmarket.stock.domain.model.*;

import java.util.List;

/**
 * DART 재무/공시 데이터 조회 포트
 */
public interface DartFinancialPort {

    List<FinancialAccount> getSingleAccount(String corpCode, String bsnsYear, String reprtCode);

    List<FinancialAccount> getMultiAccount(String corpCodes, String bsnsYear, String reprtCode);

    List<FinancialIndex> getSingleFinancialIndex(String corpCode, String bsnsYear, String reprtCode, String idxClCode);

    List<FinancialIndex> getMultiFinancialIndex(String corpCodes, String bsnsYear, String reprtCode, String idxClCode);

    List<FullFinancialStatement> getFullFinancialStatement(String corpCode, String bsnsYear, String reprtCode, String fsDiv);

    List<StockTotalQuantity> getStockTotalQuantity(String corpCode, String bsnsYear, String reprtCode);

    List<DividendInfo> getDividendInfo(String corpCode, String bsnsYear, String reprtCode);

    List<LawsuitInfo> getLawsuits(String corpCode, String bgnDe, String endDe);

    List<FundUsage> getPrivateFundUsage(String corpCode, String bsnsYear, String reprtCode);

    List<FundUsage> getPublicFundUsage(String corpCode, String bsnsYear, String reprtCode);
}
```