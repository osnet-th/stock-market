# 기업 리포트 입력 개선 Review 기록 (ce:review)

work: docs/works/2026-07-19-company-report-input-improvements-work.md
gate: docs/gates/2026-07-19-company-report-input-improvements-gates.md
issue: https://github.com/osnet-th/stock-market/issues/84
review_agents: code-simplicity-reviewer, security-sentinel, performance-oracle, architecture-strategist

## Findings (심각도 순)

### F1 [MED · 아키텍처/회귀] 주가지표 "자동" 패널 vs "내 계산" 표 불일치
- 근거: `company-report.js:873-878`(crMetricBase) ↔ 자동 카드(`priceMetrics.eps/bps`, `company-report.html` 480·1188 등).
- 변경 전(v1): 무입력 시 `내 계산 EPS = pm.eps`로 자동 카드와 정확히 일치.
- 변경 후: 무입력 시 `내 계산 EPS = perf('netIncome')/shares`, `BPS = 자본총계 term/shares`로 **항상 공식 산출**. 백엔드 `pm.eps/pm.bps`는 KRX 보고값(가중평균주식수·지배주주 기준, 비지배지분 포함 총자본 등) → 산식 기준이 달라 **인접한 자동/내계산 패널의 EPS·BPS·PER·PBR이 사용자가 아무것도 입력하지 않아도 다르게 보일 수 있음**.
- 성격: 이 기능의 의도("내 계산은 당기순이익÷유통주식수 공식")와 부합하는 측면이 있으나, baseline이 자동값과 어긋나는 회귀로 볼 여지.

### F2 [MED · 단순성] crMetricBase eps/bps 4단 폴백 사다리 중복
- 근거: `company-report.js:873-878`. eps·bps가 "입력값÷주식수 → 구버전 직접입력 → 자동값÷주식수 → 스냅샷지표"라는 동일 4단 우선순위를 분자만 바꿔 반복(3중 삼항 ×2). `code-convention.md`(변환 반복 시 helper 추출)에 해당. `perShare(...)` 헬퍼 추출로 통합 가능.

### F3 [LOW-MED · 단순성] crFormulaText가 crMetricBase 우선순위를 재복제 (drift 위험)
- 근거: `company-report.js:839,843`. "직접 입력값" 판정이 crMetricBase 폴백 우선순위를 손으로 복제 → 우선순위 변경 시 표시식 불일치. F2 헬퍼가 "채택 분기"를 반환하게 하면 함께 해소.

### F4 [LOW · 단순성] crFormulaText의 bps는 암묵적 else + magic kind
- 근거: `company-report.js:838-845`. `kind==='eps'`만 명시, 나머지는 fall-through로 BPS 텍스트. `kind==='bps'` guard 권장.

### F5 [LOW · 정확성] 연혁 YYYY.MM 월 정렬 vs 검증 불일치
- 근거: 검증 `\d{4}(\.\d{1,2})?`는 1자리 월("2024.3") 허용(`ReportManual.java`), 정렬은 `crHistoryAsc` localeCompare 문자열 → "2024.12" < "2024.3"로 12월이 3월보다 앞섬. 무손실이나 표시 순서 어긋남. 월 2자리 zero-pad 강제 또는 숫자 파싱 정렬 권장.

### F6 [LOW · 아키텍처] _crAutoEquity가 백엔드 라벨 문자열 '자본총계'에 결합
- 근거: `company-report.js:824-831`. `terms.find(t => t.label.includes('자본총계'))`로 백엔드 표시 라벨(extractor:293)에 의존. 라벨 변경/`bps` breakdown 생략 시 term 경로 실패 → 폴백(`pm.bps × snapshotShares`)으로 복구되나 취약한 크로스-레이어 계약.

### F7 [LOW · 단순성] shares=null 시 formula 텍스트와 값 불일치 엣지
- 근거: `company-report.js:837,840`. netIncome만 입력·shares 산출 불가 시 값은 pm.eps로 떨어지는데 formula는 "…÷ 유통주식수"로 표기. 실무상 shares는 대개 역산되어 low.

### 보안 (security-sentinel): 명시적 findings 없음
- requireYearOrMonth 정규식/`parseInt`(정규식 통과 후만 실행) ReDoS·크래시 없음, item·netIncome·equity 모두 길이/금액 검증 커버, x-html 미사용(x-text 자동 이스케이프), 서버 validate() 저장경로 강제 확인.
- 잔여(보안 아님): ① 음수(적자) netIncome 입력 불가 — crAmountInput/requireAmounts가 마이너스 제거(적자 기업 EPS 계산 제약, 단 기존 eps 필드도 동일 제약이라 신규 회귀는 아님). ② 프론트 월 검증 즉시 피드백 없음(서버 400 거부).

### 성능 (performance-oracle): 명시적 findings 없음
- 백엔드 쿼리/N+1 변경 없음 확정. 프론트 렌더당 crMetricBase/array.find 호출 ~3배 증가하나 대상 배열이 작고(수십 개) 트리거가 수동 폼 → sub-ms, 최적화 불필요.

## Open Questions / Assumptions
1. **F1**: 내 계산 baseline을 (a) 정직한 공식(당기순이익÷유통주식수) 유지 vs (b) 자동 보고값(pm.eps/bps)에 맞춰 정렬 vs (c) 유지 + 안내문구. → 태형님 결정.
2. **적자(음수 당기순이익/자본잠식) 입력 지원 여부** — 현재 불가. 지원하려면 netIncome/equity 입력·검증에서 선행 `-` 허용 필요.
3. 가정: v1 저장분 하위호환 무손실(4개 에이전트 공통 확인) — 재저장 시 eps/bps 값 보존, item/netIncome/equity는 신규.

## Change Summary
- 심각한 버그·데이터 손실·보안·성능 결함 없음. 하위호환 무손실 확인.
- 핵심 논점 1건(F1: 자동 vs 내계산 EPS·BPS 산식 divergence, 결정 필요)과 저위험 정리 5건(F2~F7).
