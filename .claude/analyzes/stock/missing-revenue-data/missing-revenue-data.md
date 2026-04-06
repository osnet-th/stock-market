# 해외주식 재무제표 매출(Revenue) 데이터 누락 분석

## 현재 상태

SEC EDGAR Company Facts API에서 재무제표를 가져올 때, 손익계산서의 매출(Revenue) 항목이 null로 표시되는 문제 발생. 다른 금융 사이트에서는 동일 기업의 매출 데이터가 정상 확인됨.

## 문제점

### 1. XBRL 태그명 오타 (핵심 원인)

`SecFinancialAdapter.java`에서 Revenue fallback 태그에 오타 존재:

| 위치 | 현재 코드 (오류) | 올바른 태그 |
|------|-----------------|------------|
| 223행 (연간 손익계산서) | `RevenueFromContractWithCustomersExcludingAssessedTax` | `RevenueFromContractWithCustomerExcludingAssessedTax` |
| 101행 (영업이익률 계산) | `RevenueFromContractWithCustomersExcludingAssessedTax` | `RevenueFromContractWithCustomerExcludingAssessedTax` |
| 273행 (분기 손익계산서) | `RevenueFromContractWithCustomersExcludingAssessedTax` | `RevenueFromContractWithCustomerExcludingAssessedTax` |

**Customer**(단수) vs **Customers**(복수) 차이. SEC XBRL taxonomy 공식 태그는 단수형(`Customer`).

### 2. 누락된 대체 Revenue 태그

SEC 보고 기업들이 사용하는 Revenue 관련 XBRL 태그는 다양함:

| XBRL 태그 | 사용 기업 예시 | 현재 코드 |
|-----------|-------------|----------|
| `Revenues` | Microsoft, Alphabet 등 | O (포함) |
| `RevenueFromContractWithCustomerExcludingAssessedTax` | Apple, Amazon 등 (ASC 606) | X (오타로 미동작) |
| `SalesRevenueNet` | 일부 구형 보고 기업 | X (누락) |
| `SalesRevenueGoodsNet` | 제조업체 일부 | X (누락) |
| `SalesRevenueServicesNet` | 서비스 기업 일부 | X (누락) |

## 근본 원인

- XBRL 태그명 단수/복수 오타로 ASC 606 기준 보고 기업의 매출 데이터 매칭 실패
- 대체 Revenue 태그 커버리지 부족

## 영향 범위

- **연간 재무제표**: 매출 항목 null (`buildIncomeStatement`)
- **분기 재무제표**: 매출 항목 null (`buildQuarterlyIncomeStatement`)
- **투자지표**: 영업이익률(Operating Margin) null (`getInvestmentMetrics`)
- 대부분의 미국 대형주(Apple, Amazon 등)에서 매출 데이터 미표시
