# 기업 리포트 UX 개선 2차 - Review

**Date:** 2026-07-19
**Issue:** #88
**Gate:** docs/gates/2026-07-19-company-report-ux-round2-gates.md
**대상:** work diff (company-report.js +156 / company-report.html +397−139), /code-review xhigh

## Findings (심각도 순) — 명시적 high 없음

| # | 심각도 | 위치 | 내용 |
|---|--------|------|------|
| F1 | low/med (correctness) | company-report.js:229 `_crResetDisclosures` | `_disclosureGen` 미증가 → 빠른 종목 전환 시 진행 중이던 이전 종목 공시 로드가 리셋 이후 패널에 잘못 채워질 수 있음 |
| F2 | low (correctness) | company-report.js:253 | 정정 배지가 `remark`에만 의존. DART는 정정 정기보고서를 `reportName`의 `[기재정정]` 접두로 표기하는 경우가 많아 배지 누락 |
| F3 | low (consistency) | company-report.js:1037 `crMaterialValue` | legacy/스냅샷 폴백 시 재료(당기순이익/자본총계)는 `—`인데 파생 EPS/BPS 행엔 값이 떠 모순처럼 보임 |
| F4 | low (ui) | company-report.js:1028 `crMaterialAutoHint` | 자동 안내값(선택 단위 평문)과 값 열(crAmt 조/억 접미)의 표기 형식이 한 행에서 불일치 |
| F5 | low (cleanup) | company-report.js:1012 `crAutoMaterials` | `manual` 파라미터 미사용(dead param) |
| F6 | low (efficiency) | company-report.js:1037 | 통합 표가 렌더마다 crMetricBase/crCalcMetrics ~20회 재호출(데이터 소량이라 체감 영향은 작음) |

## Open Questions / Assumptions
- ① 통합 표에서 자동 EPS/BPS 표시 기준을 "계산값(_crPerShare 우선순위)"으로 채택 → 기존 상세의 "보고 기준값(pm.eps)" 표시와 값이 달라질 수 있음(설계상 의도, brainstorm Open Q1 확정). 회귀 아님.
- 보안/성능 심각 findings 없음. 백엔드·저장 스키마 무변경으로 하위호환 리스크 낮음.

## Change Summary
- ③ 경쟁사 note textarea + 상세 줄바꿈 보존.
- ② DART 10년 정기공시 링크 바(작성+상세), 기존 API 재사용.
- ① 자동 그리드+계산기 → 단일 통합 표(작성 5단계·상세 6번), 재료 인라인 편집.

## 반영 권장
- 반영: F1(gen 증가), F2(reportName 정정 감지 병행), F4(자동 안내값도 crAmt 형식), F5(dead param 제거).
- 선택: F3(폴백 시 재료 표기 명확화), F6(렌더당 1회 계산 캐시) — 영향 작아 후순위.
