# 투자판단 제안 등급 사유 패널 Plan

**Date:** 2026-07-26
**Issue:** #93
gate: docs/gates/2026-07-26-grade-reason-panel-gates.md
**Brainstorm:** docs/brainstorms/2026-07-26-grade-reason-panel-brainstorm.md

## 범위

정량 5항목의 제안 등급 옆에 화살표 아이콘을 추가하고, 클릭 시 우측 슬라이드 패널에서 산출 사유 / 등급 기준 / 계산식 / 원천 데이터를 표시한다. 위저드 7단계 + 상세 뷰 양쪽 적용. 프런트 2파일만 수정하며 백엔드·API·기존 UI 동작(적용/제안 모두 적용/select) 불변. 테스트 코드는 미요청으로 작성하지 않음.

## 설계 요약

### company-report.js

- 상태: `crReason: { open: false, key: null }` + `crReasonOpen(key)` / `crReasonClose()` (오버레이·X·Escape 닫기)
- 표시용 기준표 상수 `crGradeCriteria` (백엔드 `GradeSuggestionCalculator` 기준표의 표시 전용 사본 — 이중 관리, 동기화 주석 명기):
  - 밴드형: assetUndervalue(청산가치·PBR 밴드), earningsUndervalue(DCF·PER 밴드)
  - 규칙형: financialHealth / profitability / growth (E→A 규칙 리스트)
- 항목별 패널 구성 config `crReasonConfig`: 사용할 ratios 키, statements 키(bs./is./cf.), breakdowns 키, valuation 섹션
- 조립 헬퍼:
  - `crReasonBasis(key)`: basis 문자열의 ` (good|warn|risk)` 마커 파싱 → {text, judgment} 리스트 (판정 색상용, 마커 없으면 중립)
  - `crReasonCriteria(key)`: 기준표 + 제안 등급 위치 하이라이트 정보
  - `crReasonFormulas(key)`: 계산식 블록 리스트 — PBR/PER는 `priceMetrics.breakdowns` terms 재사용(기존 `crBreakdownText` 조립 방식), 청산가치는 `valuation.liquidation`(조정자산 합계 − 총부채 = 청산가치, 시총/청산가치), DCF는 `valuation.dcf`+`params`(보수 = 순현금 + FCF/r 등, `calculable=false`면 reason 표기), 비율 3종·성장률은 statements 기준연도 값으로 조립 — 값 결손 시 공식만 표시
  - `crReasonSourceRows(key)`: 원천 데이터 미니 표 — snapshot.columns(연도) × 관련 statements/ratios 행, 결손 값 '—'. assetUndervalue는 청산가치 카테고리 라인(장부가×인정비율=조정가) 표
- 데이터 소스는 기존 `view === 'detail' ? detail : preview` 분기(기존 crSuggestion과 동일) — 추가 API 호출 없음

### company-report.html

- 위저드 7단계: 제안 행([적용] 옆)에 화살표 버튼 (`x-if crSuggestion` 내부, 클릭 → `crReasonOpen(item.key)`)
- 상세 뷰: 등급 카드의 "제안 X" 옆에 동일 화살표 버튼
- 파일 하단에 사유 패널 마크업 (기존 재무 상세 슬라이드 패널 패턴: 오버레이 + fixed 우측 + translate-x transition):
  1. 헤더: 항목명 + 제안 등급 배지(crGradeBadge) + 기준연도 + 닫기
  2. 산출 사유: 판정 색상 리스트 (good=green/warn=amber/risk=red, crJudgeBadge 계열 재사용)
  3. 등급 기준: A~E 밴드 바(현재 등급 하이라이트) + 규칙/밴드 설명 텍스트
  4. 계산식: mono 폰트 블록
  5. 원천 데이터: 연도별 미니 표 (금액은 crAmt 억/조 표기)

## 체크리스트

- [x] `company-report.js`: `crReason` 상태 + open/close + Escape 처리
- [x] `company-report.js`: `crGradeCriteria` 표시용 기준표 상수 (동기화 주석 포함)
- [x] `company-report.js`: `crReasonSourceConfig` + 조립 헬퍼 (basis 파싱 / 기준 / 계산식 / 원천 표)
- [x] `company-report.html`: 위저드 7단계 화살표 버튼
- [x] `company-report.html`: 상세 뷰 화살표 버튼
- [x] `company-report.html`: 우측 슬라이드 패널 마크업 (4개 섹션)
- [ ] review (Findings 정리)
- [ ] validation (JS 정적 검증 + 로컬 실행 화면 확인, 결과 기록)
- [ ] commit / push (feat/issue-93-grade-reason-panel)

## 영향 범위

- 프런트: `static/js/components/company-report.js`, `static/partials/company-report.html` 2파일. 다른 화면 영향 없음.
- 백엔드·API·Entity·DB: 동작 변경 없음. 단 리뷰 M1 반영으로 `GradeSuggestionCalculator`·`SnapshotFinancialExtractor`에 표시용 사본 역참조 주석 1줄씩 추가 (태형님 "전부 수정해줘" 승인, 2026-07-26 — gate 로그 참조).
- 기존 제안 UI(한 줄 근거·적용·제안 모두 적용): 불변 — 화살표와 패널만 additive 추가.

## 리스크

- 등급 기준표 이중 관리: 백엔드 기준 변경 시 프런트 상수 동기화 필요 — 양쪽에 상호 참조 주석으로 완화 (brainstorm에서 태형님 확인).
- 구 스냅샷·데이터 결손 리포트: 식/표가 부분 결손일 수 있음 — 공식만 표시·'—' 처리로 안전 동작.
- 패널 정보 밀도: 항목별 표시 내용은 brainstorm 확정 구성 기준 — 실제 화면 확인 후 조정은 validation 단계 피드백으로.
