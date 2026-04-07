# SEC 분기 재무제표 Q4 데이터 누락 분석

## 현재 상태

SEC 분기 재무제표 조회 시 `extractQuarterlyValues()`에서 `form == "10-Q"`만 필터링하여 Q1~Q3 데이터만 추출된다.

```
SecFinancialAdapter.java:209
.filter(e -> "10-Q".equals(e.getForm()))
```

## 문제점

- Q4 데이터가 항상 누락됨
- 다른 금융 사이트에서는 Q4 데이터가 정상 표시됨 (예: 25년 Q4)
- 최근 8분기 조회 시 실질적으로 Q1~Q3만 나와 분기 추이 비교가 불완전

## 원인 분석

SEC 제출 규정상:
- **Q1, Q2, Q3**: `10-Q` 보고서로 제출 → `form="10-Q"`, `fp="Q1"/"Q2"/"Q3"`
- **Q4**: `10-Q`로 별도 제출하지 않음. `10-K` (연간 보고서)에 포함 → `form="10-K"`, `fp="FY"`

현재 필터 `"10-Q".equals(e.getForm())`는 Q4를 포함하는 10-K를 완전히 배제한다.

## 해결 방안

### Fallback 전략 (권장)

두 가지 방법을 순서대로 적용:

**1순위 - 10-K에서 fp="Q4" 직접 추출**
- 일부 기업은 10-K 제출 시 `fp="Q4"` 엔트리를 포함
- `form="10-K" && fp="Q4"` 필터로 직접 Q4 데이터 획득

**2순위 - 계산으로 Q4 도출 (Fallback)**
- 1순위에서 Q4를 찾지 못한 경우
- 재무제표 유형별 계산 방식:
  - **손익계산서/현금흐름표** (기간 데이터): `Q4 = 10-K(FY) - Q1 - Q2 - Q3`
  - **대차대조표** (시점 데이터): `Q4 = 10-K(FY)` 값 그대로 사용

### 계산 시 주의사항
- Q1, Q2, Q3 중 하나라도 없으면 계산 불가 → Q4 = null
- 10-K(FY) 데이터가 없으면 계산 불가 → Q4 = null
- 계산된 Q4는 `fy` 기준으로 키 생성 (예: fy=2025 → "2025Q4")

## 영향 범위

| 파일 | 위치 | 변경 내용 |
|------|------|----------|
| `SecFinancialAdapter.java` | `:207-216` | `extractQuarterlyValues()` Q4 로직 추가 |
| `SecFinancialAdapter.java` | `:137-188` | `parseCompanyFacts()` 연간 데이터를 Q4 계산에 활용 |
| `SecFinancialAdapter.java` | `:193-201` | `extractAnnualValues()` 결과를 Q4 계산에 전달 |

프론트엔드, Controller, Service, DTO 변경 없음 (키 형식 "2025Q4"는 기존 `Map<String, Long>`과 호환)