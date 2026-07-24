# 투자판단 등급 자동 제안 Work 기록

**Date:** 2026-07-24
**Issue:** #91
**Gate:** docs/gates/2026-07-24-grade-suggestion-gates.md
**Plan:** docs/plans/2026-07-24-001-feat-grade-suggestion-plan.md

## 변경 파일

### 백엔드 (companyreport 패키지 내)

| 파일 | 변경 |
|------|------|
| `application/GradeSuggestionCalculator.java` | **신규.** 정량 5항목 제안 등급+근거 산출 순수 계산기. 밴드 판정(`band`/`better`), 판정 3종 집계, 다년 지속성(`presentPairs`) 헬퍼로 구성 |
| `application/dto/CompanyReportResults.java` | `GradeSuggestion(grade, basis)` 레코드 추가, `Detail`/`Preview`에 `suggestedGrades` 필드 추가 (additive) |
| `application/CompanyReportReadService.java` | `findDetail`/`preview`에서 계산기 호출해 응답에 포함 |

### 프론트

| 파일 | 변경 |
|------|------|
| `static/js/components/company-report.js` | `crSuggestion/crSuggestionBasis/crHasSuggestions/crApplySuggestion/crApplyAllSuggestions` 헬퍼 추가(작성=preview/상세=detail 소스 분기, 기존 `crBreakdownText` 패턴). draft 재개 시 preview 재구성에 `suggestedGrades` 전달 |
| `static/partials/company-report.html` | 위저드 7단계: 항목별 제안 배지+[적용]+근거(title 툴팁), 헤더 [제안 모두 적용] 버튼, 안내 문구 보강. 상세 뷰: 등급 카드에 "제안 X" 참고 표시 |

## 구현 노트

- 제안은 저장하지 않음 — `(스냅샷 × 저장 파라미터)` 순수 함수. 파라미터 조정 시 조회마다 최신 제안 반영.
- 근거 부족 항목은 map에서 생략 → 프론트는 해당 항목 UI 자체를 숨김 (구 스냅샷 하위 호환).
- 정성 2항목(사업역량·주주중시)은 계산기 대상 아님. [제안 모두 적용]도 제안 있는 항목만 적용.
- Entity·DB·스냅샷 JSON 스키마·저장 API 변경 없음.
- 코드 컨벤션: 5줄 내외 소형 메서드, guard clause. 예외: `calculate()`(항목 조립, 11줄)·`band()`(for+if 2단계) — 조립/판정 루프 성격으로 제한적 예외 (code-convention 예외 정책).
