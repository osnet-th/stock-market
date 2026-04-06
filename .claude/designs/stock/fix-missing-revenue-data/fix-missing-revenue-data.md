# 해외주식 재무제표 매출 데이터 누락 수정 설계

## 해결 방안

`SecFinancialAdapter.java`의 Revenue XBRL 태그 오타 수정 및 대체 태그 추가.

## 변경 대상

**파일**: `src/main/java/com/thlee/stock/market/stockmarket/stock/infrastructure/stock/sec/SecFinancialAdapter.java`

## 작업 리스트

- [x] 1. 연간 손익계산서 Revenue 태그 수정 (223행)
- [x] 2. 분기 손익계산서 Revenue 태그 수정 (273행)
- [x] 3. 영업이익률 계산 Revenue 태그 수정 (100-101행)

## 변경 내용

### 3곳 공통 변경

**현재 (오류)**:
```java
"Revenues", "RevenueFromContractWithCustomersExcludingAssessedTax"
```

**변경 후**:
```java
"Revenues", "RevenueFromContractWithCustomerExcludingAssessedTax",
"SalesRevenueNet", "SalesRevenueGoodsNet", "SalesRevenueServicesNet"
```

변경 사항:
1. `RevenueFromContractWithCustomers...` -> `RevenueFromContractWithCustomer...` (오타 수정)
2. `SalesRevenueNet` 추가 (ASC 606 이전 기준 보고 기업 대응)
3. `SalesRevenueGoodsNet` 추가 (제조업 기업 대응)
4. `SalesRevenueServicesNet` 추가 (서비스 기업 대응)

## 적용 위치

| 위치 | 메서드 | 설명 |
|------|--------|------|
| ~223행 | `buildIncomeStatement()` | 연간 손익계산서 매출 항목 |
| ~273행 | `buildQuarterlyIncomeStatement()` | 분기 손익계산서 매출 항목 |
| ~100-101행 | `getInvestmentMetrics()` | 영업이익률 계산용 매출 |
