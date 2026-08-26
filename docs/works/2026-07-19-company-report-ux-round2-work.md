# 기업 리포트 UX 개선 2차 - Work

**Date:** 2026-07-19
**Issue:** #88
**Gate:** docs/gates/2026-07-19-company-report-ux-round2-gates.md
**Plan:** docs/plans/2026-07-19-company-report-ux-round2-plan.md
**Branch/worktree:** feat/issue-88-company-report-ux-round2

구현 순서: Phase 3(③ 경쟁사 여러 줄) → Phase 2(② DART 링크) → Phase 1(① 주가지표 통합).

## 진행 로그

### Phase 3 (③) 경쟁사 비교 여러 줄
- 상태: 완료
- 편집: competitors `note` `<input>` → `<textarea rows=2 resize-y>`, 행 `items-center`→`items-start` (company-report.html:369-376)
- 상세: note 셀 `whitespace-pre-wrap`, name/segment `align-top` (company-report.html:1096-1098)
- JS/백엔드 무변경.

### Phase 2 (②) DART 10년 정기보고서 바로가기
- 상태: 완료 (프론트 전용)
- JS(company-report.js): `disclosures` 상태 + `_disclosureGen`; `companyReportLoadDisclosures`(types=A, 10년, 레이스 가드·같은 종목 재로드 방지), `_crResetDisclosures`, `_crYearsAgo`, `_crReportKind`(사업/반기/분기 필터), `_crReportPeriod`((YYYY.MM) 파싱), `crDisclosureByYear`(연도 desc·월 asc), `crReportKindLabel`(분기 1Q/3Q), `crReportKindClass`.
- 재사용: FinancialComponent의 `_formatYmd`·`isCorrectionDisclosure`(병합된 순수 헬퍼).
- 배선: `companyReportSelectStock`(preview와 병렬)·`_crEnterWizardFrom`·`companyReportOpenDetail`에서 로드, `companyReportOpenCreate`에서 리셋.
- HTML: 작성 위저드 상단(스텝 인디케이터 아래) + 상세 상단에 접이식 링크 바(연도 그룹, 종류별 색, 정정 마커, 원문 새 탭).
- 백엔드·API 무변경. 10년·정기공시 단일 페이지(≤100건) 충분.

### Phase 1 (①) 주가지표 자동/내 계산 통합
- 상태: 완료
- JS(company-report.js): `_crPerf`(공유), `crAutoMaterials`(입력무관 자동 재료), `crMaterialAutoHint`(편집칸 placeholder=자동값), `crMaterialValue`(유효값=override 우선). `crMetricBase`의 `perf`를 `_crPerf`로 정리(동작 동일). 미참조가 된 `crHasCustomMetrics` 제거.
- 작성 5단계(company-report.html 511~): "주가지표(자동)" 그리드 + "주가지표 계산기(내 계산)" 두 블록 → **단일 카드 통합 표**(재료 인라인 편집 6행 + 파생 8행 + 자동전용 3행 + 예상매출 지표). 재료 placeholder=자동값, 비우면 자동, 편집 즉시 재계산.
- 상세 6번(company-report.html 1258~): 자동 그리드 + 조건부 파란 "내 계산" 블록 → **단일 유효값 표**(재료 유효값·가운데 열 자동값 참고 + 파생 유효값 + 자동전용 + warnings + 예상 지표). 이중 표시 제거.
- 저장 스키마(`metricInputs` v2)·백엔드 무변경. 구 스키마(legacy eps/bps) 폴백은 `_crPerShare` 우선순위로 유지.
- 정적 검증: 구 마커 제거 확인, `<template>` 78/78 균형, 신규 참조 헬퍼 전부 정의 확인. (실동작은 validation 단계에서)

## 리뷰 반영 (F1·F2·F4·F5, 2026-07-19)
- F1: `_crResetDisclosures`에서 `_disclosureGen++`로 진행 중 로드 무효화.
- F2: 정정 감지를 `isCorrectionDisclosure(remark) || reportName.indexOf('정정')`로 병행.
- F4: `crMaterialAutoHint` 금액 안내값을 `crAmt`(조/억)로 통일, price/shares에 원/주 접미.
- F5: `crAutoMaterials`·`crMaterialAutoHint`에서 `manual` 파라미터 제거 → HTML 호출부 12곳 `(key, snapshot)`으로 치환.

## 검증 (2026-07-20)
- bootRun(dev, 8082) 기동 성공(compileJava 포함). ② 공시 실 API 200. ①③ 독립 하니스로 렌더·재계산·줄바꿈 통과. 신규 콘솔 에러 없음. 상세: docs/validations/2026-07-19-company-report-ux-round2-validation.md
