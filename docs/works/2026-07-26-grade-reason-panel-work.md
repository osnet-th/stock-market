# 투자판단 제안 등급 사유 패널 Work 기록

**Date:** 2026-07-26
**Issue:** #93
gate: docs/gates/2026-07-26-grade-reason-panel-gates.md
**Plan:** docs/plans/2026-07-26-001-feat-grade-reason-panel-plan.md

## 변경 파일

- `src/main/resources/static/js/components/company-report.js`
  - `crReason` 상태 + `crReasonOpen/Close/Label` (패널 열기·닫기)
  - `crGradeCriteria`: 표시용 등급 기준표 상수 — GradeSuggestionCalculator·SnapshotFinancialExtractor 기준의 사본 (동기화 주석 명기)
  - `crReasonBasis`: basis 문자열의 ` (good|warn|risk)` 마커 파싱 → 판정 배지
  - `crReasonFormulas` + 조립 헬퍼 6종: PBR/PER는 breakdowns 재사용(`_crReasonMetricFormula`), 청산가치(`_crReasonLiquidationFormula`), DCF(`_crReasonDcfFormula`), 비율 3종(`_crReasonRatioFormula`), 순현금(`_crReasonNetCashFormula`), 성장률(`_crReasonGrowthFormula`) — 숫자는 응답 값 그대로 나열, 프런트 재계산 없음
  - `crReasonSourceConfig` + `crReasonColumns/StatementRows/RatioRows/LiquidationLines/RiskSignals`: 항목별 원천 데이터 표 구성
  - `crReasonBaseYear`: valuationInputs.baseYear 우선, 없으면 마지막 온기 컬럼
- `src/main/resources/static/partials/company-report.html`
  - 위저드 7단계: [적용] 옆 `›` 화살표 버튼
  - 상세 뷰: "제안 X" 옆 `›` 화살표 버튼
  - 우측 슬라이드 패널 (기존 재무 상세 패널 패턴: 오버레이 + translate-x + Escape 닫기): 헤더(항목명·제안 배지·기준연도) / 산출 사유(판정 배지) / 등급 기준(A~E 바 + 규칙 하이라이트 + 판정 임계값 노트) / 계산식(mono 블록) / 원천 데이터(청산가치 라인 표·연도별 표·위험 시그널)

## 백엔드·API 변경

없음 (plan 준수 — 기존 Detail/Preview 응답만 사용, 추가 API 호출 없음).

## work 단계 자체 검증 (하네스)

- JS 문법: JavaScriptCore(`jsc`) `new Function(src)` 파싱 통과
- 브라우저 검증: 스크래치패드 목 데이터 하네스(정적 서버 :8093, Tailwind/Alpine CDN + 실제 format.js·company-report.js·파셜 사용)로 확인
  - 정량 5항목 전부 패널 오픈: 사유·기준(제안 등급 하이라이트)·계산식·원천 데이터 정상
  - 자산 저평가: 청산가치 라인 표, 청산가치 음수 시 시총/청산가치 줄 생략 확인
  - 재무건전성: 위험 시그널 칩(해당/해당 없음) 표시 확인
  - 성장성: 전년 결손 연도 '—' 표시 확인
  - 위저드·상세 뷰 양쪽 화살표 진입 + Escape 닫기 확인, 콘솔 에러 없음
- 하네스 경로: scratchpad/harness93 (저장소 외부, 커밋 대상 아님). launch.json(`issue-93-harness`)은 로컬 미추적 설정.

## 미검증 항목 (validation 단계 과제)

- 실서버(bootRun) + 실데이터 리포트에서의 확인 — company-report API는 dev 프로파일에서도 JWT principal 필요(카카오 로그인)하여 하네스로 대체함. 태형님 실사용 확인 권장.
- 모바일 폭(w-full) 실기기 확인.
