# [stocknote] StockNote 25 필드 비대화 + create() 인자 21개 swap 위험

> ce-review 2026-04-25 P1 #15 (maintainability). plan task: Phase 10 P1.

## 현재 상태

`StockNote` 도메인 클래스가 4 그룹 25 필드 흡수:
- 식별성 (8): id, userId, stockCode, marketType, exchangeCode, direction, changePercent, noteDate
- 본문 (5): triggerText, interpretationText, riskText, preReflected, initialJudgment
- **Valuation (4)**: per, pbr, evEbitda, vsAverage
- **FundamentalImpact (5)**: revenueImpact, profitImpact, cashflowImpact, oneTime, structural
- 메타 (2): createdAt, updatedAt

`create()` 인자 **21개** + `updateBody()` 인자 **14개**.

## 영향

| 위험 | 시나리오 |
|---|---|
| **인자 swap** | BigDecimal 4 인접 (per/pbr/evEbitda/changePercent), ImpactLevel 3 인접 (revenue/profit/cashflow). 호출자가 순서 바꿔 전달해도 컴파일러 미검출 |
| 호출 측 가독성 | WriteService.create 가 cmd.xxx() 21회 나열 — 매핑 누락 시 디버깅 어려움 |
| 도메인 비대화 | 한 클래스가 4 책임 그룹 흡수 — SRP 약화 |
| Mapper 동기화 | Entity↔Domain 매퍼도 21 인자 list 가 됨 |

## 해결 옵션

### 옵션 A — 도메인 model 안에 `Valuation` / `FundamentalImpact` record 신설 (권장)

도메인 record 2개 추가. StockNote 의 8개 평면 필드 → 2 VO 로 묶음. create/updateBody 인자 13개로 감소. Entity 컬럼은 평면 유지 (DB 스키마 변경 없음). Mapper 가 Entity↔VO 변환.

| 장점 | 단점 |
|---|---|
| swap 위험 해소 (VO 단위로 묶임) | 변경 범위 8 파일 |
| 도메인 가독성 회복 | 응답 평면 유지 시 vo.getPer() 호출 위임 |
| StockNote 단위 테스트 단순 | DB 스키마 무영향 |
| Entity 영향 0 | |

### 옵션 B — JPA Embeddable 로 Valuation/FundamentalImpact 흡수

도메인 record + Entity 도 Embeddable 로 묶음.

| 장점 | 단점 |
|---|---|
| Entity / Domain 일관 | DB 컬럼은 그대로지만 JPA 매핑 변경 — Hibernate 인식 검증 필요 |
| | Mapper 변환 로직 약간 단순 |

### 옵션 C — 현상 유지

| 장점 | 단점 |
|---|---|
| 변경 0 | swap 위험 그대로 |

## 추천: 옵션 A

근거:
- 변경 범위가 도메인 + 매퍼 + 호출 측에 한정 (Entity / DB 무영향)
- 응답 contract 무영향 (DetailResponse.NoteDto 평면 유지)
- swap 위험 + 가독성 동시 해소
- 옵션 B 의 JPA Embeddable 검증은 별건 plan 으로 분리 가능

## VO 설계

### Valuation
```java
public record Valuation(BigDecimal per, BigDecimal pbr, BigDecimal evEbitda, VsAverageLevel vsAverage) {
    public static Valuation empty() { return new Valuation(null, null, null, null); }
}
```

### FundamentalImpact
```java
public record FundamentalImpact(
        ImpactLevel revenueImpact,
        ImpactLevel profitImpact,
        ImpactLevel cashflowImpact,
        boolean oneTime,
        boolean structural
) {
    public static FundamentalImpact empty() { return new FundamentalImpact(null, null, null, false, false); }
}
```

> 두 VO 모두 모든 필드 nullable (사용자가 모든 필드를 채우지 않을 수 있음). `null` 자체를 허용하지 않고 `empty()` 정적 factory 로 표현.

## 호출 흐름 변경

### 변경 전

```java
StockNote.create(
        userId, stockCode, marketType, exchangeCode, direction, changePercent, noteDate, today,
        triggerText, interpretationText, riskText, preReflected, initialJudgment,
        per, pbr, evEbitda, vsAverage,            // ← swap 위험 영역
        revenueImpact, profitImpact, cashflowImpact,
        oneTime, structural
);
```

### 변경 후

```java
StockNote.create(
        userId, stockCode, marketType, exchangeCode, direction, changePercent, noteDate, today,
        triggerText, interpretationText, riskText, preReflected, initialJudgment,
        new Valuation(per, pbr, evEbitda, vsAverage),
        new FundamentalImpact(revenueImpact, profitImpact, cashflowImpact, oneTime, structural)
);
```

호출자 (WriteService.create / update) 가 cmd 에서 VO 조립.

## 코드 위치

| 파일 | 변경 |
|---|---|
| `domain/model/Valuation.java` | 신규 record |
| `domain/model/FundamentalImpact.java` | 신규 record |
| `StockNote.java` | 8개 평면 필드 → 2 VO. 생성자/create/updateBody 시그니처 수정. getter 위임 (응답 호환) |
| `StockNoteWriteService` | cmd → VO 조립 후 create/updateBody 호출 |
| `StockNoteMapper` | Entity 평면 필드 ↔ Domain VO 변환 |
| `StockNoteDetailResponse.NoteDto` | n.getValuation().per() 또는 n.getPer() (위임 getter 유지 시 무변경) |

## 호환성

- **DB 스키마 무영향**: Entity 컬럼은 평면 유지
- **응답 contract 무영향**: NoteDto 평면 필드 유지 (StockNote 가 위임 getter 보유)
- **CreateStockNoteCommand / UpdateStockNoteCommand 무영향**: application 입력은 평면

## 후속 task 와의 관계

| Task | 정합 |
|---|---|
| #16 P1 DTO 폭발 정리 | 본 task 의 도메인 VO 와 별개 — Result/Response DTO 통합 |
| 향후 옵션 B (JPA Embeddable) | 별건 plan 후보 |

## 설계 문서

[stocknote-value-objects](../../../designs/stocknote/stocknote-value-objects/stocknote-value-objects.md)
