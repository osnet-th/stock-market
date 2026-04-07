# SEC 분기 재무제표 Q4 Fallback 설계

> 모듈: stock
> 관련 분석: [sec-quarterly-q4-missing](../../../analyzes/stock/sec-quarterly-q4-missing/sec-quarterly-q4-missing.md)

## 작업 리스트

- [x] `extractQuarterlyValues()` 변경: 10-Q + 10-K(fp="Q4") 모두 추출
- [x] `parseCompanyFacts()`에서 Q4 Fallback 계산 로직 추가
- [x] Q4 Fallback 계산을 위한 private 헬퍼 메서드 추가

## 배경

SEC 분기 재무제표에서 Q4 데이터가 누락된다. 10-Q는 Q1~Q3만 제출되고, Q4는 10-K(연간)에만 포함되기 때문이다. Fallback 전략으로 Q4를 보완한다.

## 핵심 결정

### 1순위: 10-K에서 fp="Q4" 직접 추출

`extractQuarterlyValues()`의 필터를 확장하여 `10-K`의 `fp="Q4"` 엔트리도 포함한다.

```
기존: form == "10-Q"
변경: (form == "10-Q") || (form == "10-K" && fp == "Q4")
```

### 2순위: FY 기반 Q4 계산 (Fallback)

1순위에서 Q4를 찾지 못한 태그에 대해 연간(FY) 데이터로 계산한다.

- **대차대조표** (시점 데이터): `Q4 = FY` 값 그대로 사용
- **손익계산서/현금흐름표** (기간 데이터): `Q4 = FY - 누적Q3`
  - 누적Q3 = `fp="Q3"` 엔트리 중 `start`가 FY의 `start`와 동일한 것 (9개월 YTD 값)
  - 기존 `FY - Q1 - Q2 - Q3` 방식은 IPO 직후 등 Q1 미제출 시 계산 불가

대차대조표 태그 식별은 `FactEntry`의 `start` 필드로 판별한다:
- `start == null` → 시점 데이터 (대차대조표) → FY 값 그대로
- `start != null` → 기간 데이터 (손익/현금흐름) → FY - 누적Q3

### Fallback 적용 위치

`parseCompanyFacts()` 내에서 태그별로 분기 데이터 추출 직후, Q4 보완 로직을 실행한다. 기존 `extractAnnualValues()`의 연간 데이터를 활용한다.

### Fallback 실패 조건

다음 경우 Q4는 null (계산 불가):
- FY 데이터 없음
- 기간 데이터인데 Q1, Q2, Q3 중 하나라도 없음

## 구현

### SecFinancialAdapter 변경

위치: `stock/infrastructure/stock/sec/SecFinancialAdapter.java`

변경 대상:
- `extractQuarterlyValues()` (`:207-216`): 필터 조건 확장
- `parseCompanyFacts()` (`:137-188`): Q4 Fallback 계산 추가
- 새 메서드 `fillQ4Fallback()`: 태그별 Q4 보완 로직

[구현 예시](./examples/infrastructure-adapter-example.md)

## 주의사항

- 시점/기간 데이터 구분은 해당 태그의 `FactEntry.start` 필드 존재 여부로 판별
- Fallback 계산 시 Q1~Q3과 FY의 `fy` 값이 동일한 연도인지 확인 필요
- 기존 `recentQuarters` 정렬(역순)에 Q4 키가 자연스럽게 포함됨 ("2025Q4" > "2025Q3")
- 프론트엔드, Controller, Service, DTO 변경 없음
