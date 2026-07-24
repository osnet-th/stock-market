# 투자판단 정량 5항목 등급 자동 제안 Plan

**Date:** 2026-07-24
**Issue:** #91
**Gate:** docs/gates/2026-07-24-grade-suggestion-gates.md
**Brainstorm:** docs/brainstorms/2026-07-24-grade-suggestion-brainstorm.md

## 범위

정량 5항목(assetUndervalue, earningsUndervalue, financialHealth, profitability, growth)의 제안 등급(A~E)+근거를 조회 시 파생 계산해 위저드 7단계·상세 뷰에 표시한다. 최종 등급 확정(수동 select)·저장 로직은 불변. 정성 2항목 제안 없음. 테스트 코드는 미요청으로 작성하지 않음(테스트 가능한 순수 계산기 구조 유지).

## 설계 요약

- 신규 `application/GradeSuggestionCalculator` (@Component, 순수 계산): `(ReportSnapshot, ReportValuation) -> Map<String, GradeSuggestion>`
  - 입력: 스냅샷 `ratios`(판정+연도별 값)·`priceMetrics`(PER/PBR)·`riskSignals`·`valuationInputs`(netCash/baseYear), 파생 `LiquidationValuation`(시총/청산가치)·`DcfValuation`(시총/보수·낙관, FCF)
  - 기준: brainstorm의 등급 기준표 (두 신호 중 유리한 밴드, 결손 시 제안 생략)
- `CompanyReportResults`에 `GradeSuggestion(ReportGrade grade, List<String> basis)` 추가, `Detail`/`Preview` 레코드에 `suggestedGrades` 필드 추가
- `CompanyReportReadService.findDetail/preview`에서 계산기 호출해 응답에 포함
- 프론트 `company-report.js`: draft 재개 시 preview 재구성에 suggestedGrades 전달, `crSuggestion(key)`/`crApplySuggestion(key)`/`crApplyAllSuggestions()` 헬퍼
- 프론트 `company-report.html`: 7단계 select 아래 제안 배지+근거+[적용], 상단 [제안 모두 적용]; 상세 뷰 등급 카드에 "제안 X" 참고 표시

## 체크리스트

- [x] `CompanyReportResults.GradeSuggestion` 레코드 + `Detail`/`Preview`에 `suggestedGrades` 추가
- [x] `GradeSuggestionCalculator` 구현 (5항목 판정 + 근거 문자열, 코드 컨벤션 준수)
- [x] `CompanyReportReadService` 연결 (findDetail, preview)
- [x] `company-report.js` 헬퍼 + draft 재개 시 suggestedGrades 전달
- [x] `company-report.html` 위저드 7단계 제안 UI
- [x] `company-report.html` 상세 뷰 제안 참고 표시
- [x] review (Findings 정리)
- [x] validation (compileJava + JS/HTML 정적 검증, 결과 기록)
- [x] commit / push (지정 브랜치)

## 영향 범위

- 백엔드: companyreport 패키지 내 3파일 (신규 1, 수정 2). 다른 도메인 영향 없음.
- 프론트: company-report.js / company-report.html. 다른 화면 영향 없음.
- API: GET /api/company-reports/{id}, GET /api/company-reports/preview 응답에 `suggestedGrades` 추가 (additive — 기존 클라이언트 무영향).

## 리스크

- 기준치는 대화에서 승인한 값으로 하드코딩 — 조정 필요 시 후속 작업(파라미터화)으로.
- 구 스냅샷(결손 필드)에서도 제안 생략으로 안전 동작.
