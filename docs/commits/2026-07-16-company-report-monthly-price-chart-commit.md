# 기업분석리포트 월봉 주가 차트 Commit 기록

gate: docs/gates/2026-07-16-company-report-monthly-price-chart-gates.md

## 포함 파일
- src/main/java/.../stock/domain/model/ChartPeriod.java (신규)
- src/main/java/.../stock/application/dto/PriceHistoryResponse.java (신규)
- src/main/java/.../stock/domain/service/StockPricePort.java
- src/main/java/.../stock/infrastructure/stock/kis/KisStockPriceClient.java
- src/main/java/.../stock/infrastructure/stock/kis/KisStockPriceAdapter.java
- src/main/java/.../stock/application/StockPriceService.java
- src/main/java/.../stock/presentation/StockController.java
- src/main/resources/static/js/api.js
- src/main/resources/static/js/components/company-report.js
- src/main/resources/static/partials/company-report.html
- docs/brainstorms|issues|plans|works|reviews|validations|gates|commits|pushes/2026-07-16-company-report-monthly-price-chart-*.md

## 제외 파일
- 없음 (worktree 내 변경은 모두 본 작업 범위)

## 커밋 메시지
feat(companyreport): 상장일~현재 전구간 월봉 주가 차트 — 위저드·상세 (#95)

## 승인
- 태형님 승인: 예 (commit·push·PR·병합 진행 승인)
