# 해외주식 분기 재무제표(10-Q) 조회 기능 추가

> GitHub Issue: #21
> 모듈: stock

## 배경

현재 SEC 재무제표는 10-K(연간) 데이터만 필터링하여 최근 3개년을 보여준다. 최신 분기 실적(10-Q)도 조회할 수 있도록 확장하여 사용자가 최근 분기별 재무 추이를 확인할 수 있게 한다.

## 현재 구조

- `SecFinancialAdapter.extractAnnualValues()`: `form == "10-K" && fp == "FY"`로 연간만 필터링
- `ParsedCompanyFacts`: 연간 데이터만 보유 (`Map<Integer, Double>` - 연도 기준)
- `SecFinancialItem.values`: `Map<Integer, Long>` - 연도를 키로 사용
- 프론트엔드 SEC 메뉴: 손익계산서, 재무상태표, 현금흐름표, 투자지표 (모두 연간)
- API: `/api/stocks/{ticker}/sec/financial/statements` (연간 고정)

## 핵심 결정

### 1. 분기 데이터 키 설계

연간은 `연도(int)`가 키지만, 분기는 `2024Q1` 같은 형식이 필요하다. `Map<String, Long>`으로 통일하여 연간("2024")과 분기("2024Q1") 모두 지원한다.

- `SecFinancialItem.values`: `Map<Integer, Long>` → `Map<String, Long>`으로 변경
- 연간 키: `"2024"`, `"2023"`, `"2022"`
- 분기 키: `"2024Q3"`, `"2024Q2"`, `"2024Q1"`, `"2023Q4"`

### 2. API 분리 방식

기존 연간 API를 변경하지 않고, 분기 전용 엔드포인트를 추가한다.

- 기존: `GET /api/stocks/{ticker}/sec/financial/statements` (연간)
- 추가: `GET /api/stocks/{ticker}/sec/financial/statements/quarterly` (분기)

### 3. 10-Q 필터링 기준

```
form == "10-Q" && fp in ("Q1", "Q2", "Q3", "Q4")
```

- 최근 8분기(2년치) 표시
- 동일 분기 중복 시 최신 filed 기준

### 4. 캐시 확장

현재 `ParsedCompanyFacts`에 연간 데이터만 파싱하고 있다. 분기 데이터도 함께 파싱하여 캐시에 포함시킨다. SEC API 호출은 추가 없이 기존 캐시된 Company Facts 응답을 재활용한다.

### 5. 프론트엔드 연간/분기 전환

SEC 재무제표 탭에 **연간/분기 토글**을 추가한다. 기존 `sec-income`, `sec-balance`, `sec-cashflow` 메뉴는 유지하고, 토글 상태에 따라 연간/분기 데이터를 전환한다.

## 변경 대상

| 레이어 | 파일 | 변경 내용 |
|--------|------|----------|
| Domain | `SecFinancialItem.java` | `Map<Integer, Long>` → `Map<String, Long>` 변경 |
| Domain | `SecFinancialStatement.java` | 변경 없음 |
| Port | `SecFinancialPort.java` | `getQuarterlyFinancialStatements()` 추가 |
| Adapter | `SecFinancialAdapter.java` | `extractQuarterlyValues()` 추가, `ParsedCompanyFacts`에 분기 데이터 포함 |
| DTO | `SecFinancialItemResponse.java` | `Map<Integer, Long>` → `Map<String, Long>` 변경 |
| Service | `SecFinancialService.java` | `getQuarterlyFinancialStatements()` 추가 |
| Controller | `SecFinancialController.java` | `GET /{ticker}/sec/financial/statements/quarterly` 추가 |
| Frontend | `api.js` | `getSecQuarterlyStatements()` 추가 |
| Frontend | `financial.js` | 연간/분기 토글 로직, 분기 테이블 렌더링 |
| Frontend | `portfolio.js` | `secQuarterlyPeriod` 상태 추가 |
| Frontend | `index.html` | 연간/분기 토글 UI |

## 주의사항

- `SecFinancialItem.values`의 키 타입 변경(`Integer` → `String`)은 기존 연간 응답 JSON 구조에 영향. 프론트엔드 `buildSecTableRows()`도 함께 수정 필요
- 재무상태표(BALANCE)는 시점 데이터라 `start` 없이 `end`만 있음. 손익/현금흐름은 기간 데이터(`start`~`end`)
- 분기 Free Cash Flow 계산도 연간과 동일 로직 적용
- 투자지표(`sec-metrics`)는 연간 기준 유지 (분기 전환 불필요)

## 작업 리스트

- [x] `SecFinancialItem` values 키 타입 변경 (`Integer` → `String`)
- [x] `SecFinancialItemResponse` values 키 타입 변경
- [x] `SecFinancialAdapter`에 `extractQuarterlyValues()` 추가 및 `ParsedCompanyFacts` 분기 데이터 포함
- [x] `SecFinancialAdapter`에 분기 재무제표 구성 메서드 추가
- [x] `SecFinancialPort`에 `getQuarterlyFinancialStatements()` 추가
- [x] `SecFinancialService`에 `getQuarterlyFinancialStatements()` 추가
- [x] `SecFinancialController`에 분기 엔드포인트 추가
- [x] 프론트엔드: API 호출 함수 추가 (`api.js`)
- [x] 프론트엔드: 연간/분기 토글 UI 및 로직 (`financial.js`, `portfolio.js`, `index.html`)
- [x] 기존 연간 코드의 `Map<Integer, Long>` → `Map<String, Long>` 호환성 수정

## 구현 예시

- [SecFinancialItem 도메인 변경](examples/domain-model-example.md)
- [SecFinancialAdapter 분기 추출](examples/infrastructure-adapter-example.md)
- [Controller + Service 확장](examples/application-example.md)
- [프론트엔드 토글 UI](examples/frontend-example.md)
